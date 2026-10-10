package kr.co.jobhub.posting.controller;

import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.Scrap;
import kr.co.jobhub.career.repository.ScrapRepository;
import kr.co.jobhub.collection.service.CrawlService;
import kr.co.jobhub.common.service.RobotsPolicy;
import kr.co.jobhub.compensation.domain.InstitutionCompensation;
import kr.co.jobhub.compensation.repository.InstitutionCompensationRepository;
import kr.co.jobhub.posting.domain.*;
import kr.co.jobhub.posting.repository.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.URI;
import java.security.Principal;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * React 화면에 채용공고 목록·개인 스크랩·원문 미리보기 데이터를 제공하는 API다.
 * 엔티티를 그대로 노출하지 않고 화면별 응답 객체로 변환한다.
 */
@RestController
@RequestMapping("/api")
public class PostingController {
    public record PositionDto(String standardCategory, String originalName, Integer headcount,
                              String workRegion, String requirements) {}

    /** 카드 한 장에 필요한 공고 정보와 현재 회원의 개인 상태. */
    public record PostingDto(Long id, String source, String title, String organization, String region,
                             String alioInstitutionCode,
                             String employmentType, String organizationType, String publicInstitutionType,
                             String mobilityType, LocalDate postedAt, LocalDate deadline,
                             String sourceUrl, boolean scrapped, boolean applied,
                             String applicationStage, LocalDate nextStepDate, String applicationMemo,
                             List<PositionDto> positions) {
    }

    /** 현재 페이지 목록과 다음 페이지 여부 및 최근 수집 상태. */
    public record PostingPage(List<PostingDto> items, long total, int page, boolean hasNext,
                              CrawlService.Status crawlStatus) {
    }

    /** 스크랩·지원 토글 후 React가 즉시 사용할 상태. */
    public record ScrapState(boolean scrapped, boolean applied) {
    }

    /** 외부 상세 페이지의 구조와 이미지를 유지하는 격리 프레임용 HTML. */
    public record Preview(String title, String organization, String sourceUrl, String originalUrl, String html) {
    }

    /** 알리오 공시 기준 신입사원 초임. 금액 단위는 천원이다. */
    public record SalaryView(boolean available, String alioInstitutionCode, String organization,
                             Integer fiscalYear, String valueType, Long totalAmount, Long baseSalary,
                             Long fixedAllowance, Long variableAllowance, Long welfareBenefit,
                             Long performanceBonus, Long managementEvaluationBonus, Long otherAmount) {}

    public record CompetitionStage(String name, Integer applicants, Integer selected, BigDecimal ratio,
                                   String sourceType, String sourceUrl, String evidenceText,
                                   boolean calculated) {}
    public record CompetitionItem(Long postingId, String title, LocalDate postedAt, LocalDate deadline,
                                  String sourceUrl, boolean similarCategory, List<CompetitionStage> stages) {}
    public record CompetitionView(boolean available, String organization, String note,
                                  List<CompetitionItem> items) {}

    private final JobPostingRepository postings;
    private final ScrapRepository scraps;
    private final AppUserRepository users;
    private final RecruitmentPositionRepository positions;
    private final CrawlService crawler;
    private final InstitutionCompensationRepository compensations;
    private final RecruitmentCompetitionRepository competitions;
    private final Set<String> sourceHosts;

    /** 공고·회원·스크랩 저장소와 수집 상태 제공자를 주입받는다. */
    public PostingController(JobPostingRepository postings, ScrapRepository scraps,
                         AppUserRepository users, RecruitmentPositionRepository positions, CrawlService crawler,
                         InstitutionCompensationRepository compensations,
                         RecruitmentCompetitionRepository competitions,
                         @Value("${jobhub.crawl.sources}") String configuredSources) {
        this.postings = postings;
        this.scraps = scraps;
        this.users = users;
        this.positions = positions;
        this.crawler = crawler;
        this.compensations = compensations;
        this.competitions = competitions;
        this.sourceHosts = java.util.Arrays.stream(configuredSources.split(","))
                .map(raw -> raw.split("\\|", -1))
                .filter(parts -> parts.length >= 2)
                .map(parts -> URI.create(parts[1]).getHost())
                .filter(java.util.Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 인증 주체의 이메일로 회원을 확인하며 익명 요청은 거부한다. */
    private AppUser user(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** 운영자가 예약 수집의 최근 성공·실패 시각을 확인할 수 있게 한다. */
    @GetMapping("/crawl-status")
    public CrawlService.Status crawlStatus() {
        return crawler.status();
    }

    /** 검색어·마이페이지 여부·페이지 번호에 맞는 공고 20건을 반환한다. */
    @GetMapping("/postings")
    public PostingPage list(@RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "false") boolean mine,
                            @RequestParam(defaultValue = "false") boolean includeClosed,
                            @RequestParam(defaultValue = "") String region,
                            @RequestParam(defaultValue = "") String regionFull,
                            @RequestParam(defaultValue = "") String district,
                            @RequestParam(defaultValue = "") String mobility,
                            @RequestParam(defaultValue = "") String jobCategory, Principal principal) {
        long userId = principal == null ? 0L : user(principal).id;
        int safePage = Math.max(0, page);
        var result = postings.search(q, mine, includeClosed, LocalDate.now(ZoneId.of("Asia/Seoul")), region, regionFull,
                district, mobility, jobCategory, userId,
                PageRequest.of(safePage, 20));
        Map<Long, Scrap> saved = scraps.findByUserId(userId).stream()
                .collect(Collectors.toMap(s -> s.posting.getId(), Function.identity()));
        List<PostingDto> items = result.getContent().stream()
                .map(p -> {
                    Scrap s = saved.get(p.id);
                    String publicInstitutionType = p.publicInstitutionType;
                    // 서비스에서는 법적 설립 주체보다 사용자가 요청한 실제 근무 이동 범위를 우선한다.
                    if ("PUBLIC".equals(p.organizationType)) {
                        publicInstitutionType = "ROTATIONAL".equals(p.mobilityType) ? "CENTRAL_PUBLIC"
                                : "FIXED".equals(p.mobilityType) ? "LOCAL_PUBLIC" : null;
                    }
                    return new PostingDto(p.id, p.source, p.title, p.organization, p.region,
                            p.alioInstitutionCode,
                            p.employmentType, p.organizationType, publicInstitutionType,
                            p.mobilityType == null ? "UNKNOWN" : p.mobilityType,
                            p.postedAt, p.deadline, p.sourceUrl,
                            s != null, s != null && s.applied,
                            s == null || s.stage == null ? "SAVED" : s.stage,
                            s == null ? null : s.nextStepDate, s == null ? "" : s.memo,
                            positions.findByPostingIdOrderById(p.id).stream()
                                    .map(rp -> new PositionDto(rp.standardCategory, rp.originalName, rp.headcount,
                                            rp.workRegion, rp.requirements)).toList());
                })
                .toList();
        return new PostingPage(items, result.getTotalElements(), safePage, result.hasNext(), crawler.status());
    }

    /** 마감된 과거 공고를 기관·직렬·지역·연도·고용형태로 검색한다. */
    @GetMapping("/postings/archive")
    public PostingPage archive(@RequestParam(defaultValue = "") String q,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "") String region,
                               @RequestParam(defaultValue = "") String jobCategory,
                               @RequestParam(defaultValue = "") String employmentType,
                               @RequestParam(required = false) Integer year,
                               Principal principal) {
        long userId = principal == null ? 0L : user(principal).id;
        int safePage = Math.max(0, page);
        int currentYear = LocalDate.now(ZoneId.of("Asia/Seoul")).getYear();
        if (year != null && (year < 1990 || year > currentYear)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "채용연도를 확인해 주세요.");
        }
        LocalDate fromDate = year == null ? null : LocalDate.of(year, 1, 1);
        LocalDate toDate = year == null ? null : LocalDate.of(year, 12, 31);
        var result = postings.searchArchive(q, LocalDate.now(ZoneId.of("Asia/Seoul")), region,
                employmentType, fromDate, toDate, jobCategory, PageRequest.of(safePage, 20));
        Map<Long, Scrap> saved = scraps.findByUserId(userId).stream()
                .collect(Collectors.toMap(s -> s.posting.getId(), Function.identity()));
        List<PostingDto> items = result.getContent().stream().map(p -> postingDto(p, saved.get(p.id))).toList();
        return new PostingPage(items, result.getTotalElements(), safePage, result.hasNext(), crawler.status());
    }

    private PostingDto postingDto(JobPosting p, Scrap s) {
        String publicInstitutionType = p.publicInstitutionType;
        if ("PUBLIC".equals(p.organizationType)) {
            publicInstitutionType = "ROTATIONAL".equals(p.mobilityType) ? "CENTRAL_PUBLIC"
                    : "FIXED".equals(p.mobilityType) ? "LOCAL_PUBLIC" : null;
        }
        return new PostingDto(p.id, p.source, p.title, p.organization, p.region, p.alioInstitutionCode,
                p.employmentType, p.organizationType, publicInstitutionType,
                p.mobilityType == null ? "UNKNOWN" : p.mobilityType, p.postedAt, p.deadline, p.sourceUrl,
                s != null, s != null && s.applied, s == null || s.stage == null ? "SAVED" : s.stage,
                s == null ? null : s.nextStepDate, s == null ? "" : s.memo,
                positions.findByPostingIdOrderById(p.id).stream()
                        .map(rp -> new PositionDto(rp.standardCategory, rp.originalName, rp.headcount,
                                rp.workRegion, rp.requirements)).toList());
    }

    /** 공고의 알리오 기관코드로 가장 최근 신입사원 초임 공시를 조회한다. */
    @GetMapping("/postings/{id}/salary")
    public SalaryView salary(@PathVariable Long id) {
        JobPosting posting = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String code = posting.alioInstitutionCode;
        if (code == null || code.isBlank()) {
            return new SalaryView(false, null, posting.organization, null, null,
                    null, null, null, null, null, null, null, null);
        }
        return compensations.findFirstByAlioInstitutionCodeOrderByFiscalYearDesc(code)
                .map(value -> new SalaryView(true, code, value.organization, value.fiscalYear, value.valueType,
                        value.totalAmount, value.baseSalary, value.fixedAllowance, value.variableAllowance,
                        value.welfareBenefit, value.performanceBonus, value.managementEvaluationBonus,
                        value.otherAmount))
                .orElseGet(() -> new SalaryView(false, code, posting.organization, null, null,
                        null, null, null, null, null, null, null, null));
    }

    /** 같은 기관의 최근 2년 공고 중 직렬이 겹치는 자료를 우선해 공개된 경쟁률을 반환한다. */
    @GetMapping("/postings/{id}/competition")
    public CompetitionView competition(@PathVariable Long id) {
        JobPosting current = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (current.alioInstitutionCode == null || current.alioInstitutionCode.isBlank()) {
            return new CompetitionView(false, current.organization, "알리오 기관코드가 없는 공고입니다.", List.of());
        }
        Set<String> currentCategories = categorySet(positions.findByPostingIdOrderById(id));
        LocalDate cutoff = LocalDate.now(ZoneId.of("Asia/Seoul")).minusYears(2);
        List<CompetitionItem> similar = new ArrayList<>();
        List<CompetitionItem> regular = new ArrayList<>();
        for (JobPosting candidate : postings.findByAlioInstitutionCodeOrderByPostedAtDesc(current.alioInstitutionCode)) {
            if (candidate.id.equals(id) || candidate.postedAt == null || candidate.postedAt.isBefore(cutoff)) continue;
            List<RecruitmentCompetition> rows = competitions.findByPostingIdOrderById(candidate.id);
            if (rows.isEmpty()) continue;
            boolean categoryMatch = !java.util.Collections.disjoint(
                    currentCategories, categorySet(positions.findByPostingIdOrderById(candidate.id)));
            CompetitionItem item = new CompetitionItem(candidate.id, candidate.title, candidate.postedAt,
                    candidate.deadline, candidate.sourceUrl, categoryMatch,
                    rows.stream().map(row -> new CompetitionStage(row.stageName, row.applicants,
                            row.selected, row.ratio, row.sourceType, row.sourceUrl,
                            row.evidenceText, row.calculated)).toList());
            if (categoryMatch) similar.add(item);
            else if (isRegularEmployment(candidate.employmentType)) regular.add(item);
        }
        // 동일 직렬 자료가 하나라도 있으면 다른 직렬을 섞지 않는다. 없을 때만 기관 정규직 자료로 대체한다.
        List<CompetitionItem> selected = new ArrayList<>(similar.isEmpty() ? regular : similar);
        if (selected.size() > 8) selected = new ArrayList<>(selected.subList(0, 8));
        String note = !similar.isEmpty()
                ? "같은 기관·유사 직렬의 최근 2년 공개자료를 우선 표시합니다."
                : !regular.isEmpty()
                ? "동일 직렬 자료가 없어 같은 기관의 정규직 경쟁률을 참고자료로 표시합니다."
                : "최근 2년 동안 공개된 동일 직렬 또는 정규직 경쟁률 자료가 없습니다.";
        return new CompetitionView(!selected.isEmpty(), current.organization, note, selected);
    }

    private Set<String> categorySet(List<RecruitmentPosition> rows) {
        return rows.stream().map(row -> row.standardCategory).filter(java.util.Objects::nonNull)
                .flatMap(value -> java.util.Arrays.stream(value.split("[,，]")))
                .map(String::trim).filter(value -> !value.isBlank()).collect(Collectors.toSet());
    }

    private boolean isRegularEmployment(String employmentType) {
        if (employmentType == null) return false;
        return java.util.Arrays.stream(employmentType.split("[,，]"))
                .map(String::trim)
                .anyMatch(value -> value.equals("정규직") || value.startsWith("정규직("));
    }

    /** 공고 스크랩을 토글한다. 해제하면 개인 지원 완료 상태도 함께 제거된다. */
    @PostMapping("/postings/{id}/scrap")
    public ScrapState toggleScrap(@PathVariable Long id, Principal principal) {
        AppUser user = user(principal);
        JobPosting posting = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var existing = scraps.findByUserIdAndPostingId(user.id, id);
        if (existing.isPresent()) {
            scraps.delete(existing.get());
            return new ScrapState(false, false);
        }
        Scrap s = new Scrap();
        s.user = user;
        s.posting = posting;
        scraps.save(s);
        return new ScrapState(true, false);
    }

    /** 스크랩한 공고의 지원 완료 여부와 적용 시각을 토글한다. */
    @PostMapping("/postings/{id}/applied")
    public ScrapState toggleApplied(@PathVariable Long id, Principal principal) {
        Scrap s = scraps.findByUserIdAndPostingId(user(principal).id, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scrap first"));
        s.applied = !s.applied;
        s.appliedAt = s.applied ? Instant.now() : null;
        scraps.save(s);
        return new ScrapState(true, s.applied);
    }

    /**
     * 등록된 잡알리오 상세 페이지의 HTML 구조, 표, 이미지 및 스타일시트를 보존한다.
     * 신뢰할 수 있는 출처만 읽고 활성 콘텐츠를 제거하여 sandbox iframe에 제공한다.
     */
    @GetMapping("/postings/{id}/preview")
    public Preview preview(@PathVariable Long id) {
        JobPosting p = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        URI uri = URI.create(p.sourceUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || !sourceHosts.contains(uri.getHost().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "미리보기를 제공할 수 없습니다.");
        }
        try {
            Document doc = Jsoup.connect(p.sourceUrl).userAgent("Mozilla/5.0 PublicJobHub/0.3")
                    .followRedirects(false).maxBodySize(3_000_000).timeout(12000).get();
            // 기관 채용사이트 주소는 개별 공고가 아닌 통합 홈페이지인 경우가 많다.
            // 따라서 실제 공고문 PDF를 우선하고, 없으면 JOB-ALIO 개별 상세 페이지를 사용한다.
            String originalUrl = p.sourceUrl;
            if (postingPdf(doc) != null) {
                originalUrl = "/api/postings/" + p.id + "/document";
            }
            Element original = doc.selectFirst("#contentRV, main, article");
            if (original == null) {
                original = doc.body();
            }
            Element content = original.clone();
            content.select("script, iframe, object, embed, form, meta, base").remove();
            for (Element element : content.getAllElements()) {
                for (var attribute : List.copyOf(element.attributes().asList())) {
                    String key = attribute.getKey().toLowerCase();
                    if (key.startsWith("on")) {
                        element.removeAttr(attribute.getKey());
                    } else if (List.of("href", "src", "poster", "data-src").contains(key)) {
                        String absolute = element.absUrl(attribute.getKey());
                        if (absolute.startsWith("https://") || absolute.startsWith("http://")) {
                            element.attr(attribute.getKey(), absolute);
                        } else {
                            element.removeAttr(attribute.getKey());
                        }
                    } else if (key.equals("srcset")) {
                        element.removeAttr(attribute.getKey());
                    }
                }
                if (element.hasAttr("data-src") && !element.hasAttr("src")) {
                    element.attr("src", element.attr("data-src"));
                }
                if (element.normalName().equals("a")) {
                    element.attr("target", "_blank").attr("rel", "noopener noreferrer");
                }
            }
            StringBuilder styles = new StringBuilder();
            for (Element link : doc.select("head link[rel=stylesheet]")) {
                String href = link.absUrl("href");
                if (href.startsWith("https://") || href.startsWith("http://")) {
                    styles.append("<link rel=\"stylesheet\" href=\"")
                            .append(org.jsoup.nodes.Entities.escape(href)).append("\">");
                }
            }
            String html = "<!doctype html><html lang=\"ko\"><head><meta charset=\"utf-8\">"
                    + "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; "
                    + "img-src https: http: data:; style-src https: http: 'unsafe-inline'; "
                    + "font-src https: http: data:;\">"
                    + styles
                    + "<style>html,body{margin:0;padding:0;background:white;}"
                    + "body{overflow-x:auto;}#contentRV,#contentRV #txt{width:auto!important;min-width:0!important;"
                    + "max-width:none!important;margin:0!important;padding:12px!important;}"
                    + "img{max-width:100%;height:auto;}table{max-width:100%;}"
                    + ".hidden,.hide,[style*='display:none'],[style*='display: none']{display:block!important;}"
                    + "a[href]{cursor:pointer!important;pointer-events:auto!important;}</style></head><body>"
                    + content.outerHtml() + "</body></html>";
            return new Preview(p.title, p.organization, p.sourceUrl, originalUrl, html);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "원문을 가져오지 못했습니다.");
        }
    }

    /** 공고문 PDF를 브라우저가 작은 창 안에서 표시하도록 inline 응답으로 중계한다. */
    @GetMapping("/postings/{id}/document")
    public ResponseEntity<?> document(@PathVariable Long id) {
        JobPosting p = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            Document detail = Jsoup.connect(p.sourceUrl).userAgent("Mozilla/5.0 PublicJobHub/0.3")
                    .followRedirects(false).maxBodySize(3_000_000).timeout(12000).get();
            String pdfUrl = postingPdf(detail);
            if (pdfUrl == null) {
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(p.sourceUrl)).build();
            }
            URI pdfUri = URI.create(pdfUrl);
            if (!"https".equalsIgnoreCase(pdfUri.getScheme())
                    || !("alio.go.kr".equalsIgnoreCase(pdfUri.getHost())
                    || "www.alio.go.kr".equalsIgnoreCase(pdfUri.getHost()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            var response = Jsoup.connect(pdfUrl).userAgent("Mozilla/5.0 PublicJobHub/0.3")
                    .ignoreContentType(true).maxBodySize(20_000_000).timeout(20000).execute();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=job-posting.pdf")
                    .body(response.bodyAsBytes());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "공고문을 가져오지 못했습니다.");
        }
    }

    /** JOB-ALIO 상세 페이지에서 실제 채용 공고문 PDF 링크를 찾는다. */
    private String postingPdf(Document doc) {
        for (Element attachment : doc.select("#contentRV a[href]")) {
            String label = attachment.text().trim().toLowerCase();
            String candidate = attachment.absUrl("href");
            if (label.contains("공고문") && label.contains("pdf")
                    && (candidate.startsWith("https://") || candidate.startsWith("http://"))) {
                return candidate;
            }
        }
        return null;
    }
}
