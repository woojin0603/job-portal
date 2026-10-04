package kr.co.jobhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.jobhub.model.JobPosting;
import kr.co.jobhub.model.RecruitmentPosition;
import kr.co.jobhub.repo.JobPostingRepository;
import kr.co.jobhub.repo.RecruitmentPositionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.*;
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
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;
    private final String serviceKey;
    private final boolean enabled;
    private final int pages;
    private final int rows;
    private final long delayMillis;
    private volatile Status status = new Status(null, null, 0, null);

    public PublicRecruitmentApiService(JobPostingRepository postings, RecruitmentPositionRepository positions,
                                       ObjectMapper json,
                                       @Value("${jobhub.public-data.recruitment.base-url}") String baseUrl,
                                       @Value("${jobhub.public-data.recruitment.service-key:}") String serviceKey,
                                       @Value("${jobhub.public-data.recruitment.enabled:false}") boolean enabled,
                                       @Value("${jobhub.public-data.recruitment.pages:2}") int pages,
                                       @Value("${jobhub.public-data.recruitment.rows:100}") int rows,
                                       @Value("${jobhub.public-data.recruitment.request-delay-millis:250}") long delayMillis) {
        this.postings = postings;
        this.positions = positions;
        this.json = json;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.serviceKey = serviceKey.trim();
        this.enabled = enabled;
        this.pages = Math.max(1, pages);
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
        int count = 0;
        try {
            for (int page = 1; page <= pages; page++) {
                JsonNode response = get("/list", Map.of("resultType", "json", "ongoingYn", "Y",
                        "pageNo", Integer.toString(page), "numOfRows", Integer.toString(rows)));
                requireSuccess(response);
                JsonNode result = response.path("result");
                if (!result.isArray() || result.isEmpty()) break;
                for (JsonNode summary : result) {
                    long sn = summary.path("recrutPblntSn").asLong(0);
                    if (sn == 0) continue;
                    JsonNode detailResponse = get("/detail", Map.of("resultType", "json", "sn", Long.toString(sn)));
                    requireSuccess(detailResponse);
                    save(detailResponse.path("result"));
                    count++;
                    Thread.sleep(delayMillis);
                }
                if (result.size() < rows) break;
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

    private JsonNode get(String path, Map<String, String> parameters) throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + path).queryParam("serviceKey", serviceKey);
        parameters.forEach(builder::queryParam);
        URI uri = builder.build().encode().toUri();
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("채용 API HTTP " + response.statusCode());
        return json.readTree(response.body());
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
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        return value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
    }

    private Integer nullableInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private LocalDate date(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE); }
        catch (Exception ignored) { return null; }
    }
}
