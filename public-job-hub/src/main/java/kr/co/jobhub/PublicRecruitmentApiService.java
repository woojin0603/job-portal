package kr.co.jobhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.jobhub.model.JobPosting;
import kr.co.jobhub.model.RecruitmentCompetition;
import kr.co.jobhub.model.RecruitmentPosition;
import kr.co.jobhub.repo.JobPostingRepository;
import kr.co.jobhub.repo.RecruitmentCompetitionRepository;
import kr.co.jobhub.repo.RecruitmentPositionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 재정경제부 공공기관 채용정보 API를 DB 캐시로 동기화한다. */
@Service
public class PublicRecruitmentApiService {
    public record Status(Instant lastAttempt, Instant lastSuccess, int lastCount, String error) {}

    private static final String SOURCE = "MOEF-RECRUITMENT-API";
    private final JobPostingRepository postings;
    private final RecruitmentPositionRepository positions;
    private final RecruitmentCompetitionRepository competitions;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;
    private final String serviceKey;
    private final boolean enabled;
    private final int pages;
    private final int historyPages;
    private final int rows;
    private final long delayMillis;
    private volatile Status status = new Status(null, null, 0, null);

    public PublicRecruitmentApiService(JobPostingRepository postings, RecruitmentPositionRepository positions,
                                       RecruitmentCompetitionRepository competitions,
                                       ObjectMapper json,
                                       @Value("${jobhub.public-data.recruitment.base-url}") String baseUrl,
                                       @Value("${jobhub.public-data.recruitment.service-key:}") String serviceKey,
                                       @Value("${jobhub.public-data.recruitment.enabled:false}") boolean enabled,
                                       @Value("${jobhub.public-data.recruitment.pages:2}") int pages,
                                       @Value("${jobhub.public-data.recruitment.history-pages:2}") int historyPages,
                                       @Value("${jobhub.public-data.recruitment.rows:100}") int rows,
                                       @Value("${jobhub.public-data.recruitment.request-delay-millis:250}") long delayMillis) {
        this.postings = postings;
        this.positions = positions;
        this.competitions = competitions;
        this.json = json;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.serviceKey = normalizeServiceKey(serviceKey);
        this.enabled = enabled;
        this.pages = Math.max(1, pages);
        this.historyPages = Math.max(0, historyPages);
        this.rows = Math.max(1, Math.min(100, rows));
        this.delayMillis = Math.max(100, delayMillis);
    }

    public Status status() { return status; }

    /** 기존 HTML 수집 직후 실행해 API를 화면 요청과 분리하고 일일 호출량을 제한한다. */
    @Scheduled(cron = "0 15 0 * * *", zone = "Asia/Seoul")
    public synchronized Status sync() {
        Instant attempt = Instant.now();
        if (!enabled) return status = new Status(attempt, null, 0, "API 수집이 비활성화되어 있습니다.");
        if (serviceKey.isBlank()) return status = new Status(attempt, null, 0, "JOBHUB_PUBLIC_DATA_API_KEY가 없습니다.");
        if (serviceKey.contains("JOBHUB_PUBLIC_DATA_"))
            return status = new Status(attempt, null, 0,
                    "인증키 값에 다른 환경변수가 함께 들어 있습니다. IntelliJ에서 환경변수를 별도 행으로 등록하세요.");
        int count = 0;
        try {
            count += syncListing(Map.of("ongoingYn", "Y"), pages, false);
            if (historyPages > 0) {
                LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                count += syncListing(Map.of(
                        "ongoingYn", "N",
                        "pbancBgngYmd", today.minusYears(2).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        "pbancEndYmd", today.format(DateTimeFormatter.ISO_LOCAL_DATE)), historyPages, true);
            }
            status = new Status(attempt, Instant.now(), count, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status = new Status(attempt, null, count, "API 수집이 중단되었습니다.");
        } catch (Exception e) {
            status = new Status(attempt, null, count, e.getMessage());
        }
        return status;
    }

    private int syncListing(Map<String, String> filters, int pageLimit, boolean skipCached) throws Exception {
        int count = 0;
        for (int page = 1; page <= pageLimit; page++) {
            Map<String, String> parameters = new LinkedHashMap<>(filters);
            parameters.put("resultType", "json");
            parameters.put("pageNo", Integer.toString(page));
            parameters.put("numOfRows", Integer.toString(rows));
            JsonNode response = get("/list", parameters);
            requireSuccess(response);
            JsonNode result = response.path("result");
            if (!result.isArray() || result.isEmpty()) break;
            for (JsonNode summary : result) {
                long sn = summary.path("recrutPblntSn").asLong(0);
                if (sn == 0 || (skipCached && hasCachedCompetition(sn))) continue;
                JsonNode detailResponse = get("/detail", Map.of("resultType", "json", "sn", Long.toString(sn)));
                requireSuccess(detailResponse);
                save(detailResponse.path("result"));
                count++;
                Thread.sleep(delayMillis);
            }
            if (result.size() < rows) break;
        }
        return count;
    }

    private boolean hasCachedCompetition(long sn) {
        String sourceId = Long.toString(sn);
        String detailUrl = "https://job.alio.go.kr/mobile2021/recruit/recruitView.do?idx=" + sourceId;
        return postings.findBySourceAndSourceId(SOURCE, sourceId)
                .or(() -> postings.findFirstBySourceUrl(detailUrl))
                .map(posting -> competitions.existsByPostingId(posting.id))
                .orElse(false);
    }

    private JsonNode get(String path, Map<String, String> parameters) throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + path);
        parameters.forEach(builder::queryParam);
        String parameterUrl = builder.build().encode().toUriString();
        // Encoding 키는 브라우저에서 성공한 문자열을 그대로 사용하고, Decoding 키만 한 번 인코딩한다.
        String encodedKey = serviceKey.contains("%")
                ? serviceKey
                : UriUtils.encodeQueryParam(serviceKey, StandardCharsets.UTF_8);
        URI uri = URI.create(parameterUrl + (parameterUrl.contains("?") ? "&" : "?") + "serviceKey=" + encodedKey);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("채용 API HTTP " + response.statusCode() + apiErrorMessage(response.body()));
        }
        return json.readTree(response.body());
    }

    /** IntelliJ가 값 전체를 따옴표로 감싼 경우에도 실제 인증키만 사용한다. */
    private String normalizeServiceKey(String value) {
        String key = value == null ? "" : value.trim();
        while (key.length() >= 2 && ((key.startsWith("\"") && key.endsWith("\""))
                || (key.startsWith("'") && key.endsWith("'")))) {
            key = key.substring(1, key.length() - 1).trim();
        }
        if (key.regionMatches(true, 0, "serviceKey=", 0, "serviceKey=".length()))
            key = key.substring("serviceKey=".length()).trim();
        return key;
    }

    /** 인증키를 노출하지 않고 게이트웨이가 보낸 오류 코드와 메시지만 상태 화면에 전달한다. */
    private String apiErrorMessage(String body) {
        if (body == null || body.isBlank()) return "";
        try {
            JsonNode response = json.readTree(body);
            String code = firstText(response, "resultCode", "returnReasonCode", "errCd");
            String message = firstText(response, "resultMsg", "returnAuthMsg", "errMsg");
            if (code != null || message != null) {
                return " (" + String.join(": ",
                        java.util.stream.Stream.of(code, message).filter(Objects::nonNull).toList()) + ")";
            }
        } catch (Exception ignored) {
            // XML/HTML 응답은 아래의 알려진 오류명만 추출한다.
        }
        for (String known : List.of("SERVICE_ACCESS_DENIED_ERROR", "SERVICE_KEY_IS_NOT_REGISTERED_ERROR",
                "DEADLINE_HAS_EXPIRED_ERROR", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR",
                "PERMISSION_DENIED", "SERVICE_KEY_IS_NULL")) {
            if (body.contains(known)) return " (" + known + ")";
        }
        return "";
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode found = node.findValue(field);
            if (found != null) {
                String value = found.asText("").trim();
                if (!value.isBlank()) return value;
            }
        }
        return null;
    }

    private void requireSuccess(JsonNode response) {
        if (response.path("resultCode").asInt(-1) != 200)
            throw new IllegalStateException("채용 API 오류: " + response.path("resultMsg").asText("알 수 없는 오류"));
    }

    private void save(JsonNode item) {
        String sn = text(item, "recrutPblntSn");
        if (sn == null || text(item, "recrutPbancTtl") == null || text(item, "instNm") == null) return;
        String title = text(item, "recrutPbancTtl");
        String organization = text(item, "instNm");
        LocalDate postedAt = date(text(item, "pbancBgngYmd"));
        LocalDate deadline = date(text(item, "pbancEndYmd"));
        String detailUrl = "https://job.alio.go.kr/mobile2021/recruit/recruitView.do?idx=" + sn;
        JobPosting posting = postings.findBySourceAndSourceId(SOURCE, sn)
                .or(() -> postings.findFirstBySourceUrl(detailUrl))
                .or(() -> postings.findFirstByOrganizationAndTitleAndPostedAtAndDeadline(
                        organization, title, postedAt, deadline))
                .orElseGet(JobPosting::new);
        if (posting.id == null) {
            posting.source = SOURCE;
            posting.sourceId = sn;
        }
        posting.title = title;
        posting.organization = organization;
        posting.alioInstitutionCode = text(item, "pblntInstCd");
        posting.publicAdminStandardInstitutionCode = text(item, "pbadmsStdInstCd");
        posting.region = text(item, "workRgnNmLst");
        posting.employmentType = text(item, "hireTypeNmLst");
        posting.organizationType = "PUBLIC";
        posting.mobilityType = posting.mobilityType == null ? "UNKNOWN" : posting.mobilityType;
        posting.postedAt = postedAt;
        posting.deadline = deadline;
        posting.open = "Y".equalsIgnoreCase(text(item, "ongoingYn"))
                || posting.deadline == null || !posting.deadline.isBefore(LocalDate.now(ZoneId.of("Asia/Seoul")));
        posting.sourceUrl = detailUrl;
        posting.updatedAt = Instant.now();
        postings.save(posting);

        positions.deleteByPostingId(posting.id);
        Map<String, Integer> headcounts = new LinkedHashMap<>();
        for (JsonNode step : item.path("steps")) {
            String name = text(step, "recrutPbancTtl");
            if (name != null) headcounts.putIfAbsent(name, nullableInt(step, "recrutNope"));
        }
        if (headcounts.isEmpty()) headcounts.put(posting.title, nullableInt(item, "recrutNope"));
        for (var entry : headcounts.entrySet()) {
            RecruitmentPosition position = new RecruitmentPosition();
            position.posting = posting;
            position.standardCategory = Optional.ofNullable(text(item, "ncsCdNmLst")).orElse("기타");
            position.originalName = entry.getKey();
            position.headcount = entry.getValue();
            position.workRegion = posting.region;
            position.requirements = text(item, "aplyQlfcCn");
            positions.save(position);
        }

        competitions.deleteByPostingId(posting.id);
        int sequence = 1;
        for (JsonNode step : item.path("steps")) {
            Integer applicants = nullableInt(step, "aplyNope");
            Integer selected = Optional.ofNullable(nullableInt(step, "recrutNope"))
                    .orElse(nullableInt(step, "slctNope"));
            java.math.BigDecimal ratio = decimal(step, "cmpttRt");
            if (applicants == null && selected == null && (ratio == null || ratio.signum() == 0)) continue;
            RecruitmentCompetition competition = new RecruitmentCompetition();
            competition.posting = posting;
            competition.stageName = Optional.ofNullable(text(step, "recrutPbancTtl"))
                    .orElse(Optional.ofNullable(text(step, "recrutStepNm")).orElse("전형 " + sequence));
            competition.applicants = applicants;
            competition.selected = selected;
            competition.ratio = ratio != null && ratio.signum() > 0 ? ratio : null;
            competitions.save(competition);
            sequence++;
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        return value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
    }

    private Integer nullableInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private java.math.BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isNumber()) return value.decimalValue();
        String text = value.asText("").replace(":1", "").trim();
        try { return text.isBlank() ? null : new java.math.BigDecimal(text); }
        catch (NumberFormatException ignored) { return null; }
    }

    private LocalDate date(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE); }
        catch (Exception ignored) { return null; }
    }
}
