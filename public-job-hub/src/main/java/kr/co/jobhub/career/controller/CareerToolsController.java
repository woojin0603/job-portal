package kr.co.jobhub.career.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.*;
import kr.co.jobhub.career.repository.*;
import kr.co.jobhub.posting.domain.*;
import kr.co.jobhub.posting.repository.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.*;
import java.util.*;

/** 스펙 매칭, 지원 일정, 맞춤 공고 조건을 한 API 묶음으로 제공한다. */
@RestController
@RequestMapping("/api/tools")
public class CareerToolsController {
    private static final Set<String> STAGES = Set.of(
            "SAVED", "PREPARING", "SUBMITTED", "DOCUMENT_PASSED", "WRITTEN_TEST",
            "INTERVIEW", "PASSED", "REJECTED");
    private static final List<String> STANDARD_JOB_CATEGORIES = List.of(
            "행정·사무", "기획·전략", "인사·노무", "법무", "회계·재무", "홍보·마케팅",
            "전산·IT", "데이터·통계", "정보보안", "연구", "교육", "문화·예술",
            "토목", "건축", "전기", "전자", "기계", "통신", "화학", "환경", "에너지",
            "안전·보건", "시설관리", "운전·운송", "생산·정비", "농림·축산",
            "의료·보건", "사회복지", "상담", "고객서비스", "영업·사업관리", "기타·전문직");
    private static final Map<String, List<String>> FIELD_KEYWORDS = Map.ofEntries(
            Map.entry("행정·사무", List.of("행정", "사무", "총무", "문서", "민원")),
            Map.entry("기획·전략", List.of("기획", "전략", "성과", "정책", "사업기획")),
            Map.entry("인사·노무", List.of("인사", "노무", "채용", "급여", "조직관리")),
            Map.entry("법무", List.of("법무", "법률", "소송", "계약", "변호사")),
            Map.entry("회계·재무", List.of("회계", "재무", "세무", "결산", "예산")),
            Map.entry("홍보·마케팅", List.of("홍보", "마케팅", "콘텐츠", "언론", "브랜드")),
            Map.entry("전산·IT", List.of("전산", "IT", "정보처리", "소프트웨어", "시스템", "개발")),
            Map.entry("데이터·통계", List.of("데이터", "통계", "빅데이터", "분석", "SQL")),
            Map.entry("정보보안", List.of("정보보안", "보안", "사이버", "개인정보", "네트워크")),
            Map.entry("연구", List.of("연구", "R&D", "실험", "조사")),
            Map.entry("교육", List.of("교육", "교수", "강의", "훈련", "교원")),
            Map.entry("문화·예술", List.of("문화", "예술", "공연", "전시", "디자인")),
            Map.entry("토목", List.of("토목", "측량", "지적", "도로", "구조")),
            Map.entry("건축", List.of("건축", "건설", "시공", "설계", "감리")),
            Map.entry("전기", List.of("전기", "전력", "배전", "송전", "전기설비")),
            Map.entry("전자", List.of("전자", "회로", "반도체", "제어", "계측")),
            Map.entry("기계", List.of("기계", "기계설비", "공조", "냉동", "자동차")),
            Map.entry("통신", List.of("통신", "전파", "무선", "네트워크", "방송")),
            Map.entry("화학", List.of("화학", "화공", "분석화학", "공정", "소재")),
            Map.entry("환경", List.of("환경", "수질", "대기", "폐기물", "기후")),
            Map.entry("에너지", List.of("에너지", "신재생", "발전", "원자력", "가스")),
            Map.entry("안전·보건", List.of("안전", "보건", "소방", "산업위생", "재난")),
            Map.entry("시설관리", List.of("시설", "시설관리", "유지보수", "영선", "설비관리")),
            Map.entry("운전·운송", List.of("운전", "운송", "철도", "물류", "교통")),
            Map.entry("생산·정비", List.of("생산", "정비", "품질", "제조", "보전")),
            Map.entry("농림·축산", List.of("농업", "농림", "산림", "축산", "수산")),
            Map.entry("의료·보건", List.of("의료", "간호", "보건", "약무", "임상")),
            Map.entry("사회복지", List.of("사회복지", "복지", "돌봄", "재활", "복지사")),
            Map.entry("상담", List.of("상담", "심리", "상담사", "사례관리", "청소년")),
            Map.entry("고객서비스", List.of("고객", "서비스", "고객지원", "안내", "콜센터")),
            Map.entry("영업·사업관리", List.of("영업", "사업관리", "수주", "계약관리", "거래처")),
            Map.entry("기타·전문직", List.of("전문", "전문직")));
    private static final Map<String, List<String>> CERTIFICATE_ALIASES = Map.ofEntries(
            Map.entry("컴퓨터활용능력", List.of("컴활", "컴퓨터활용능력")),
            Map.entry("한국사능력검정", List.of("한국사능력검정", "한국사검정", "한능검")),
            Map.entry("SQL개발자", List.of("SQLD", "SQL개발자")),
            Map.entry("데이터분석준전문가", List.of("ADSP", "데이터분석준전문가")),
            Map.entry("데이터분석전문가", List.of("ADP", "데이터분석전문가")),
            Map.entry("정보처리", List.of("정보처리", "정보처리기사", "정보처리산업기사")),
            Map.entry("워드프로세서", List.of("워드프로세서", "워드")),
            Map.entry("전산세무", List.of("전산세무", "전산세무회계")),
            Map.entry("전산회계", List.of("전산회계", "전산세무회계")));

    public record ApplicationRequest(String stage, LocalDate nextStepDate, @Size(max = 2000) String memo) {}
    public record ApplicationState(String stage, LocalDate nextStepDate, String memo) {}
    public record ApplicationSpec(@Size(max = 5000) String certifications,
                                  @Size(max = 5000) String careers,
                                  @Size(max = 5000) String activities,
                                  @Size(max = 5000) String degrees,
                                  @Size(max = 3000) String notes) {}
    public record ScheduleItem(Long id, String type, String label, LocalDate date,
                               boolean tentative, String evidence, String sourceUrl) {}
    public record ApplicationItem(Long id, String title, String postingTitle, String organization, LocalDate deadline,
                                  String sourceUrl, String stage, LocalDate nextStepDate, String memo,
                                  ApplicationSpec applicationSpec, List<ScheduleItem> schedules) {}
    public record PreferenceRequest(@Size(max = 500) String keywords, @Size(max = 500) String regions,
                                    @Size(max = 100) String mobilityTypes, boolean enabled,
                                    boolean newPostingAlerts, boolean deadlineAlerts,
                                    @Size(max = 30) String deadlineDays,
                                    Boolean postingChangeAlerts, Boolean importantChangeOnly) {}
    public record Preference(String keywords, String regions, String mobilityTypes,
                             boolean enabled, boolean newPostingAlerts, boolean deadlineAlerts,
                             String deadlineDays, boolean postingChangeAlerts,
                             boolean importantChangeOnly, Instant lastViewedAt) {}
    public record AlertItem(Long id, String title, String organization, String region,
                            LocalDate deadline, String mobilityType, String sourceUrl, boolean fresh,
                            Long remainingDays, String reason) {}
    public record Recommendation(Long id, String title, String organization, LocalDate deadline,
                                 String sourceUrl, int score, List<String> reasons) {}
    public record Eligibility(String status, String detail) {}
    public record MatchComponent(String key, String label, int score, int maxScore,
                                 String status, List<String> details) {}
    public record MatchResult(List<String> matchedCertifications, List<String> requirementEvidence,
                              List<String> strengths, List<String> gaps, int score, String summary,
                              Eligibility eligibility, List<MatchComponent> components,
                              List<Recommendation> recommendations) {}
    public record FilterOptions(List<String> regions, List<String> mobilityTypes,
                                List<String> jobCategories) {}

    private final AppUserRepository users;
    private final JobPostingRepository postings;
    private final ScrapRepository scraps;
    private final UserProfileRepository profiles;
    private final JobAlertPreferenceRepository preferences;
    private final RecruitmentPositionRepository positions;
    private final RecruitmentScheduleRepository schedules;
    private final ObjectMapper json;

    public CareerToolsController(AppUserRepository users, JobPostingRepository postings,
                                 ScrapRepository scraps, UserProfileRepository profiles,
                                 JobAlertPreferenceRepository preferences,
                                 RecruitmentPositionRepository positions,
                                 RecruitmentScheduleRepository schedules, ObjectMapper json) {
        this.users = users;
        this.postings = postings;
        this.scraps = scraps;
        this.profiles = profiles;
        this.preferences = preferences;
        this.positions = positions;
        this.schedules = schedules;
        this.json = json;
    }

    @PostMapping("/postings/{id}/application")
    public ApplicationState application(@PathVariable Long id, @Valid @RequestBody ApplicationRequest request,
                                        Principal principal) {
        AppUser user = user(principal);
        if (!STAGES.contains(request.stage())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원 단계를 확인해 주세요.");
        }
        JobPosting posting = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Scrap scrap = scraps.findByUserIdAndPostingId(user.id, id).orElseGet(() -> {
            Scrap created = new Scrap();
            created.user = user;
            created.posting = posting;
            return created;
        });
        scrap.stage = request.stage();
        scrap.nextStepDate = request.nextStepDate();
        scrap.memo = request.memo() == null ? "" : request.memo().trim();
        scrap.applied = !Set.of("SAVED", "PREPARING").contains(scrap.stage);
        scrap.appliedAt = scrap.applied && scrap.appliedAt == null ? Instant.now() : scrap.appliedAt;
        scraps.save(scrap);
        return new ApplicationState(scrap.stage, scrap.nextStepDate, scrap.memo);
    }

    /** 회원이 보관한 모든 공고를 지원 단계와 일정순으로 반환한다. */
    @GetMapping("/applications")
    public List<ApplicationItem> applications(Principal principal) {
        AppUser user = user(principal);
        return scraps.findWithPostingByUserId(user.id).stream()
                .sorted(Comparator
                        .comparing((Scrap scrap) -> scrap.nextStepDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(scrap -> scrap.posting.deadline,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(scrap -> new ApplicationItem(
                        scrap.posting.id,
                        scrap.posting.title,
                        scrap.posting.title,
                        scrap.posting.organization,
                        scrap.posting.deadline,
                        scrap.posting.sourceUrl,
                        scrap.stage == null ? "SAVED" : scrap.stage,
                        scrap.nextStepDate,
                        scrap.memo == null ? "" : scrap.memo,
                        readApplicationSpec(scrap.applicationProfile),
                        schedules.findByPostingIdOrderByEventDateAsc(scrap.posting.id).stream()
                                .map(this::scheduleItem).toList()))
                .toList();
    }

    /** 해당 공고에 실제 제출한 스펙을 개인 기본 스펙과 분리해 저장한다. */
    @PostMapping("/postings/{id}/application-profile")
    public ApplicationSpec applicationProfile(@PathVariable Long id,
                                               @Valid @RequestBody ApplicationSpec request,
                                               Principal principal) {
        AppUser user = user(principal);
        JobPosting posting = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Scrap scrap = scraps.findByUserIdAndPostingId(user.id, id).orElseGet(() -> {
            Scrap created = new Scrap();
            created.user = user;
            created.posting = posting;
            return created;
        });
        ApplicationSpec cleaned = new ApplicationSpec(clean(request.certifications()), clean(request.careers()),
                clean(request.activities()), clean(request.degrees()), clean(request.notes()));
        try {
            scrap.applicationProfile = json.writeValueAsString(cleaned);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원 스펙을 저장하지 못했습니다.");
        }
        scraps.save(scrap);
        return cleaned;
    }

    private ApplicationSpec readApplicationSpec(String value) {
        if (value == null || value.isBlank()) return new ApplicationSpec("", "", "", "", "");
        try {
            return json.readValue(value, ApplicationSpec.class);
        } catch (Exception ignored) {
            return new ApplicationSpec("", "", "", "", "");
        }
    }

    @GetMapping("/preferences")
    public Preference preference(Principal principal) {
        AppUser user = user(principal);
        return preferences.findByUserId(user.id).map(this::preference)
                .orElseGet(() -> new Preference("", "", "", true, true, true,
                        "7,3,1", true, false, null));
    }

    @PostMapping("/preferences")
    public Preference savePreference(@Valid @RequestBody PreferenceRequest request, Principal principal) {
        AppUser user = user(principal);
        JobAlertPreference value = preferences.findByUserId(user.id).orElseGet(JobAlertPreference::new);
        value.user = user;
        value.keywords = clean(request.keywords());
        value.regions = clean(request.regions());
        value.mobilityTypes = clean(request.mobilityTypes());
        value.enabled = request.enabled();
        value.newPostingAlerts = request.newPostingAlerts();
        value.deadlineAlerts = request.deadlineAlerts();
        value.deadlineDays = normalizedDeadlineDays(request.deadlineDays());
        value.postingChangeAlerts = request.postingChangeAlerts() == null || request.postingChangeAlerts();
        value.importantChangeOnly = request.importantChangeOnly() != null && request.importantChangeOnly();
        value.updatedAt = Instant.now();
        return preference(preferences.save(value));
    }

    @GetMapping("/alerts")
    public List<AlertItem> alerts(Principal principal) {
        AppUser user = user(principal);
        JobAlertPreference pref = preferences.findByUserId(user.id).orElse(null);
        if (pref == null || !pref.enabled || !pref.deadlineAlerts) return List.of();
        Set<Integer> deadlineDays = deadlineDays(pref.deadlineDays);
        Set<Long> scrappedPostingIds = scraps.findByUserId(user.id).stream()
                .map(scrap -> scrap.posting.id)
                .collect(java.util.stream.Collectors.toSet());
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<AlertItem> result = new ArrayList<>(postings.findAll().stream()
                .filter(p -> scrappedPostingIds.contains(p.id))
                .filter(p -> p.deadline == null || !p.deadline.isBefore(today))
                .filter(p -> {
                    long remaining = p.deadline == null ? Long.MIN_VALUE
                            : java.time.temporal.ChronoUnit.DAYS.between(today, p.deadline);
                    return deadlineDays.contains((int) remaining);
                })
                .sorted(Comparator.comparing((JobPosting p) -> p.deadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(30)
                .map(p -> {
                    boolean fresh = pref.lastViewedAt == null || p.updatedAt.isAfter(pref.lastViewedAt);
                    Long remaining = p.deadline == null ? null
                            : java.time.temporal.ChronoUnit.DAYS.between(today, p.deadline);
                    String reason = remaining != null && remaining == 0 ? "오늘 마감" : "마감 " + remaining + "일 전";
                    return new AlertItem(p.id, p.title, p.organization, p.region, p.deadline, p.mobilityType,
                            p.sourceUrl, fresh, remaining, reason);
                })
                .toList());
        if (!scrappedPostingIds.isEmpty()) {
            Map<Long, JobPosting> postingById = postings.findAllById(scrappedPostingIds).stream()
                    .collect(java.util.stream.Collectors.toMap(p -> p.id, p -> p));
            List<Long> ids = new ArrayList<>(scrappedPostingIds);
            schedules.findByPostingIdInAndEventDateGreaterThanEqualOrderByEventDateAsc(ids, today).stream()
                    .filter(value -> deadlineDays.contains((int) java.time.temporal.ChronoUnit.DAYS.between(today, value.eventDate)))
                    .forEach(value -> {
                        JobPosting p = postingById.get(value.posting.getId());
                        if (p == null) return;
                        long remaining = java.time.temporal.ChronoUnit.DAYS.between(today, value.eventDate);
                        String timing = remaining == 0 ? "오늘" : remaining + "일 전";
                        result.add(new AlertItem(p.id, p.title, p.organization, p.region, value.eventDate,
                                p.mobilityType, value.sourceUrl,
                                pref.lastViewedAt == null || value.detectedAt.isAfter(pref.lastViewedAt),
                                remaining, value.label + " · " + timing));
                    });
        }
        return result.stream().sorted(Comparator.comparing(AlertItem::deadline,
                Comparator.nullsLast(Comparator.naturalOrder()))).limit(60).toList();
    }

    private ScheduleItem scheduleItem(RecruitmentSchedule value) {
        return new ScheduleItem(value.id, value.scheduleType, value.label, value.eventDate,
                value.tentative, value.evidence, value.sourceUrl);
    }

    @PostMapping("/alerts/read")
    public void readAlerts(Principal principal) {
        AppUser user = user(principal);
        JobAlertPreference pref = preferences.findByUserId(user.id).orElseGet(JobAlertPreference::new);
        pref.user = user;
        pref.lastViewedAt = Instant.now();
        pref.updatedAt = Instant.now();
        preferences.save(pref);
    }

    @GetMapping("/filters")
    public FilterOptions filters() {
        List<String> regions = postings.findAll().stream().map(p -> p.region)
                .filter(Objects::nonNull).filter(value -> !value.isBlank()).distinct().sorted().toList();
        List<String> categories = java.util.stream.Stream.concat(STANDARD_JOB_CATEGORIES.stream(),
                        positions.findAll().stream().map(p -> p.standardCategory)
                                .filter(Objects::nonNull)
                                .flatMap(value -> Arrays.stream(value.split("[,，]")))
                                .map(String::trim).filter(value -> !value.isBlank()))
                .distinct().toList();
        return new FilterOptions(regions, List.of("ROTATIONAL", "FIXED", "UNKNOWN"), categories);
    }

    @GetMapping("/postings/{id}/match")
    public MatchResult match(@PathVariable Long id, Principal principal) {
        AppUser user = user(principal);
        JobPosting posting = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ProfileSignals signals = profileSignals(user);
        List<String> evidence = new ArrayList<>();
        StringBuilder sourceText = new StringBuilder(clean(posting.title)).append(' ')
                .append(clean(posting.organization)).append(' ').append(clean(posting.region))
                .append(' ').append(clean(posting.employmentType));
        StringBuilder roleText = new StringBuilder(clean(posting.title)).append(' ')
                .append(clean(posting.employmentType));
        positions.findByPostingIdOrderById(posting.id).forEach(position -> {
            sourceText.append(' ').append(clean(position.standardCategory))
                    .append(' ').append(clean(position.originalName))
                    .append(' ').append(clean(position.requirements));
            roleText.append(' ').append(clean(position.standardCategory))
                    .append(' ').append(clean(position.originalName))
                    .append(' ').append(clean(position.requirements));
            if (position.requirements != null && !position.requirements.isBlank() && evidence.size() < 4) {
                evidence.add("자격요건: " + shorten(position.requirements.replaceAll("\\s+", " "), 500));
            }
        });
        try {
            Document doc = Jsoup.connect(posting.sourceUrl).userAgent("PublicJobHub/0.4")
                    .maxBodySize(3_000_000).timeout(12000).get();
            sourceText.append(' ').append(doc.text());
            for (Element heading : doc.select("#contentRV h4, main h2, main h3, article h2, article h3")) {
                String label = heading.text();
                if (label.contains("자격") || label.contains("우대") || label.contains("전형")) {
                    Element next = heading.nextElementSibling();
                    if (next != null) evidence.add(label + ": " + shorten(next.text(), 500));
                }
                if (evidence.size() == 6) break;
            }
        } catch (Exception ignored) {
            // 원문 접근이 일시적으로 실패해도 DB에 저장된 자격요건으로 매칭을 계속한다.
        }
        String text = sourceText.toString();
        List<String> requiredLines = certificateClauses(evidence, false);
        List<String> preferredLines = certificateClauses(evidence, true);
        String requiredText = String.join(" ", requiredLines);
        String preferredText = String.join(" ", preferredLines);
        boolean requiredCertificateFound = requiredLines.stream().anyMatch(this::containsCertificateRequirement);
        List<String> requiredCertificates = signals.certificates().stream()
                .filter(name -> certificateMatches(requiredText, name)).distinct().toList();
        List<String> bonusCertificates = signals.certificates().stream()
                .filter(name -> certificateMatches(preferredText, name)).distinct().toList();
        List<String> matched = signals.certificates().stream()
                .filter(name -> certificateMatches(text, name)).distinct().toList();
        List<String> relatedCertificates = matched.stream()
                .filter(name -> !requiredCertificates.contains(name) && !bonusCertificates.contains(name)).toList();
        List<String> strengths = new ArrayList<>();
        requiredCertificates.forEach(name -> strengths.add("필수 자격요건과 일치: " + name));
        bonusCertificates.forEach(name -> strengths.add("가산점·우대 자격증에 명시: " + name));
        relatedCertificates.forEach(name -> strengths.add("직무 관련 자격증: " + name));
        List<String> careerFieldMatches = signals.careerFields().stream()
                .filter(field -> fieldRelated(roleText.toString(), field))
                .distinct().toList();
        careerFieldMatches.forEach(field -> strengths.add("직무 관련 경력 분야: " + field));
        List<String> activityFieldMatches = signals.activityFields().stream()
                .filter(field -> fieldRelated(roleText.toString(), field))
                .distinct().toList();
        activityFieldMatches.forEach(field -> strengths.add("직무 관련 대외활동 분야: " + field));

        boolean requiredMatched = !requiredCertificates.isEmpty();
        Eligibility eligibility = !requiredCertificateFound
                ? new Eligibility("필수 자격증 제한 미확인", "공고 분석 문구에서 특정 필수 자격증을 확인하지 못했습니다. 학력·경력 등 다른 지원요건은 원문을 확인하세요.")
                : requiredMatched
                ? new Eligibility("필수 자격증 일치", "보유 자격증이 공고의 필수 자격요건 문구와 직접 일치합니다. 등급·취득시점 등 세부 조건은 원문을 확인하세요.")
                : new Eligibility("필수 자격증 불일치 가능", "공고에 필수 자격증 조건이 있으나 등록한 보유 자격증과 직접 일치하지 않았습니다.");

        int qualificationScore = requiredCertificateFound
                ? (requiredMatched ? 50 : 0)
                : Math.min(35, bonusCertificates.size() * 20 + relatedCertificates.size() * 10);
        int careerScore = fieldRelevanceScore(roleText.toString(), signals.careerFields(), 30);
        int activityScore = fieldRelevanceScore(roleText.toString(), signals.activityFields(), 20);
        int score = qualificationScore + careerScore + activityScore;
        List<MatchComponent> components = List.of(
                new MatchComponent("QUALIFICATION", "필수 자격증", qualificationScore, 50,
                        eligibility.status(), java.util.stream.Stream.of(
                                requiredLines.stream().limit(3).map(v -> "공고 필수조건: " + shorten(v, 180)),
                                requiredCertificates.stream().map(v -> "필수 자격 일치: " + v),
                                bonusCertificates.stream().map(v -> "가산점·우대: " + v),
                                relatedCertificates.stream().map(v -> "직무 관련: " + v))
                                .flatMap(s -> s).toList()),
                new MatchComponent("CAREER", "경력 분야 연관성", careerScore, 30,
                        signals.careerFields().isEmpty() ? "분야 선택안함" :
                                careerFieldMatches.isEmpty() ? "직무 연관성 미확인" : "직무 관련",
                        fieldMatchDetails(roleText.toString(), careerFieldMatches)),
                new MatchComponent("ACTIVITY", "대외활동 분야 연관성", activityScore, 20,
                        signals.activityFields().isEmpty() ? "분야 선택안함" :
                                activityFieldMatches.isEmpty() ? "직무 연관성 미확인" : "직무 관련",
                        fieldMatchDetails(roleText.toString(), activityFieldMatches)));
        List<String> gaps = new ArrayList<>();
        if (requiredCertificateFound && !requiredMatched)
            gaps.add("공고의 필수 자격증 조건과 등록한 보유 자격증이 직접 일치하지 않습니다.");
        if (signals.certificates().isEmpty()) gaps.add("등록된 자격증이 없어 자격요건 비교가 제한됩니다.");
        String summary = strengths.isEmpty()
                ? "등록 정보와 공고문에서 직접 확인되는 일치 근거가 없습니다. 반드시 원문 자격요건을 확인해 주세요."
                : "필수 자격증·경력 분야·대외활동 분야를 분리해 " + strengths.size() + "개의 관련 근거를 확인했습니다.";
        return new MatchResult(matched, evidence, strengths, gaps, score, summary,
                eligibility, components,
                recommendations(posting.id, signals));
    }

    private List<Recommendation> recommendations(Long excludedPostingId, ProfileSignals signals) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Map<Long, List<RecruitmentPosition>> positionsByPosting = positions.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(value -> value.posting.getId()));
        return postings.findAll().stream()
                .filter(value -> !Objects.equals(value.id, excludedPostingId))
                .filter(value -> value.open && (value.deadline == null || !value.deadline.isBefore(today)))
                .map(value -> recommendation(value, positionsByPosting.getOrDefault(value.id, List.of()), signals))
                .filter(value -> value.score() > 0)
                .sorted(Comparator.comparingInt(Recommendation::score).reversed()
                        .thenComparing(Recommendation::deadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(4).toList();
    }

    private Recommendation recommendation(JobPosting posting, List<RecruitmentPosition> postingPositions,
                                          ProfileSignals signals) {
        String positionText = postingPositions.stream()
                .map(value -> clean(value.standardCategory) + " " + clean(value.originalName) + " " + clean(value.requirements))
                .collect(java.util.stream.Collectors.joining(" "));
        String source = posting.title + " " + posting.organization + " " + positionText;
        Set<String> sourceTokens = meaningfulTokens(List.of(source));
        List<String> reasons = new ArrayList<>();
        int score = 0;
        for (String certificate : signals.certificates()) if (normalizedContains(source, certificate)) {
            score += 30; reasons.add("자격증 " + certificate + " 관련");
        }
        for (String major : signals.majors()) if (normalizedContains(source, major)
                || meaningfulTokens(List.of(major)).stream().anyMatch(sourceTokens::contains)) {
            score += 20; reasons.add("전공 " + major + " 관련");
        }
        for (String experience : signals.experiences()) {
            List<String> matches = meaningfulTokens(List.of(experience)).stream()
                    .filter(sourceTokens::contains).limit(2).toList();
            if (!matches.isEmpty()) {
                score += Math.min(12, matches.size() * 6);
                reasons.add("경력: " + String.join(", ", matches));
            }
        }
        for (String field : signals.careerFields()) if (fieldRelated(source, field)) {
            score += 15;
            reasons.add("경력 분야 " + field + " 관련");
        }
        for (String field : signals.activityFields()) if (fieldRelated(source, field)) {
            score += 10;
            reasons.add("대외활동 분야 " + field + " 관련");
        }
        String profileText = String.join(" ", signals.certificates()) + " "
                + String.join(" ", signals.majors()) + " " + String.join(" ", signals.experiences()) + " "
                + String.join(" ", signals.careerFields()) + " " + String.join(" ", signals.activityFields());
        Map<String, List<String>> relatedFields = new LinkedHashMap<>();
        relatedFields.put("전산·데이터 역량 관련", List.of("정보처리", "컴퓨터", "소프트웨어", "데이터", "SQL", "빅데이터", "전산", "IT"));
        relatedFields.put("회계·재무 역량 관련", List.of("회계", "재경", "세무", "재무"));
        relatedFields.put("전기 직무 관련", List.of("전기", "전력"));
        relatedFields.put("기계 직무 관련", List.of("기계", "설비"));
        relatedFields.put("안전 직무 관련", List.of("산업안전", "안전관리", "보건"));
        relatedFields.put("환경 직무 관련", List.of("환경", "수질", "대기", "폐기물"));
        for (var field : relatedFields.entrySet()) {
            boolean profileMatch = field.getValue().stream().anyMatch(value -> contains(profileText, value));
            boolean postingMatch = field.getValue().stream().anyMatch(value -> contains(source, value));
            if (profileMatch && postingMatch) { score += 18; reasons.add(field.getKey()); }
        }
        return new Recommendation(posting.id, posting.title, posting.organization, posting.deadline,
                posting.sourceUrl, Math.min(100, score), reasons.stream().distinct().limit(3).toList());
    }

    private record ProfileSignals(List<String> certificates, List<String> majors,
                                  List<String> experiences, List<String> careerFields,
                                  List<String> activityFields) {}

    private ProfileSignals profileSignals(AppUser user) {
        try {
            UserProfile profile = profiles.findByUserId(user.id).orElse(null);
            if (profile == null) return new ProfileSignals(List.of(), List.of(), List.of(), List.of(), List.of());
            List<String> certificates = new ArrayList<>();
            for (JsonNode item : json.readTree(profile.certifications == null ? "[]" : profile.certifications)) {
                String name = item.path("name").asText().trim();
                String grade = item.path("grade").asText().trim();
                if (!name.isBlank()) {
                    boolean technicalGrade = List.of("기술사", "기능장", "기사", "산업기사", "기능사")
                            .contains(grade);
                    certificates.add(technicalGrade && !name.endsWith(grade) ? name + grade : name);
                }
            }
            List<String> majors = new ArrayList<>();
            for (JsonNode item : json.readTree(profile.degree == null ? "[]" : profile.degree)) {
                String major = item.path("major").asText().trim();
                if (!major.isBlank()) majors.add(major);
            }
            List<String> experiences = new ArrayList<>();
            List<String> careerFields = new ArrayList<>();
            for (String raw : List.of(profile.careerHistory == null ? "" : profile.careerHistory)) {
                if (raw == null || raw.isBlank()) continue;
                for (JsonNode item : json.readTree(raw)) {
                    String selectedField = item.path("field").asText().trim();
                    if (!selectedField.isBlank()) careerFields.add(selectedField);
                    for (String field : List.of("companyName", "position", "duties")) {
                        String value = item.path(field).asText().trim();
                        if (value.length() >= 2) experiences.add(value);
                    }
                }
            }
            List<String> activityFields = new ArrayList<>();
            if (profile.activities != null && !profile.activities.isBlank()) {
                for (JsonNode item : json.readTree(profile.activities)) {
                    String selectedField = item.path("field").asText().trim();
                    if (!selectedField.isBlank()) activityFields.add(selectedField);
                }
            }
            return new ProfileSignals(certificates.stream().distinct().toList(),
                    majors.stream().distinct().toList(), experiences.stream().distinct().toList(),
                    careerFields.stream().distinct().toList(), activityFields.stream().distinct().toList());
        } catch (Exception e) {
            return new ProfileSignals(List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private Preference preference(JobAlertPreference value) {
        return new Preference(clean(value.keywords), clean(value.regions), clean(value.mobilityTypes),
                value.enabled, value.newPostingAlerts, value.deadlineAlerts,
                normalizedDeadlineDays(value.deadlineDays), value.postingChangeAlerts == null || value.postingChangeAlerts,
                value.importantChangeOnly != null && value.importantChangeOnly, value.lastViewedAt);
    }
    private Set<String> csv(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (String value : raw.split(",")) if (!value.trim().isBlank()) result.add(value.trim());
        return result;
    }
    private boolean contains(String text, String value) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }
    private boolean normalizedContains(String text, String value) {
        if (text == null || value == null || value.isBlank()) return false;
        String source = text.toLowerCase(Locale.ROOT).replaceAll("[^가-힣a-z0-9+#]", "");
        String target = value.toLowerCase(Locale.ROOT).replaceAll("[^가-힣a-z0-9+#]", "");
        return target.length() >= 2 && source.contains(target);
    }
    private boolean containsCertificateRequirement(String value) {
        if (value == null) return false;
        return java.util.regex.Pattern.compile(
                "자격증|면허|기술사|기능장|산업기사|기사|기능사|자격\\s*(?:소지|보유|취득)")
                .matcher(value).find();
    }
    private List<String> certificateClauses(Collection<String> values, boolean preferred) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            String segmented = value.replaceAll("(?=(?:우대사항|우대조건|가산점|가점))", ";");
            for (String clause : segmented.split("(?<=[.!?])\\s+|[\\n\\r;•]+")) {
                String cleaned = clean(clause).replaceAll("\\s+", " ");
                if (cleaned.isBlank() || !containsCertificateRequirement(cleaned)) continue;
                boolean preference = cleaned.contains("우대") || cleaned.contains("가점")
                        || cleaned.contains("가산") || cleaned.contains("선택");
                boolean explicitRequired = cleaned.contains("필수") || cleaned.contains("지원자격")
                        || cleaned.contains("응시자격") || cleaned.contains("자격요건");
                if (preferred ? preference : (!preference && explicitRequired)) result.add(cleaned);
            }
        }
        return result.stream().distinct().limit(8).toList();
    }
    private boolean certificateMatches(String text, String certificate) {
        if (normalizedContains(text, certificate)) return true;
        String compact = certificate == null ? "" : certificate
                .replaceAll("\\([^)]*\\)", " ")
                .replaceAll("(?i)(?:심화|기본)?\\s*[1-6]급|단일등급", " ")
                .trim();
        if (compact.length() >= 3 && normalizedContains(text, compact)) return true;
        String normalizedCertificate = compact.toLowerCase(Locale.ROOT).replaceAll("[^가-힣a-z0-9]", "");
        return CERTIFICATE_ALIASES.entrySet().stream()
                .filter(entry -> normalizedCertificate.contains(
                        entry.getKey().toLowerCase(Locale.ROOT).replaceAll("[^가-힣a-z0-9]", "")))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(alias -> normalizedContains(text, alias));
    }
    private boolean fieldRelated(String roleText, String field) {
        if (field == null || field.isBlank() || "선택안함".equals(field)) return false;
        List<String> keywords = FIELD_KEYWORDS.getOrDefault(field,
                meaningfulTokens(List.of(field)).stream().toList());
        return keywords.stream().anyMatch(keyword -> normalizedContains(roleText, keyword));
    }
    private List<String> fieldMatchDetails(String roleText, List<String> fields) {
        List<String> details = new ArrayList<>();
        for (String field : fields) {
            List<String> keywords = FIELD_KEYWORDS.getOrDefault(field, List.of(field)).stream()
                    .filter(keyword -> normalizedContains(roleText, keyword)).distinct().limit(3).toList();
            details.add(keywords.isEmpty() ? field : field + " · 공고 근거: " + String.join(", ", keywords));
        }
        return details;
    }
    private int fieldRelevanceScore(String roleText, List<String> fields, int maxScore) {
        if (fields == null || fields.isEmpty()) return 0;
        int bestEvidence = 0;
        for (String field : fields) {
            if (field == null || field.isBlank() || "선택안함".equals(field)) continue;
            List<String> keywords = FIELD_KEYWORDS.getOrDefault(field, List.of(field));
            int matches = (int) keywords.stream().filter(keyword -> normalizedContains(roleText, keyword)).count();
            if (normalizedContains(roleText, field)) matches += 2;
            bestEvidence = Math.max(bestEvidence, matches);
        }
        if (bestEvidence >= 3) return maxScore;
        if (bestEvidence == 2) return Math.round(maxScore * 0.8f);
        if (bestEvidence == 1) return Math.round(maxScore * 0.6f);
        return 0;
    }
    private Set<String> meaningfulTokens(Collection<String> values) {
        Set<String> stop = Set.of("담당업무", "업무", "근무", "경력", "활동", "관련", "지원", "채용",
                "직원", "기관", "회사", "관리", "기타", "이상", "이하", "해당", "우대", "필수");
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) continue;
            for (String token : value.toLowerCase(Locale.ROOT).split("[^가-힣a-z0-9+#]+")) {
                if (token.length() >= 2 && !stop.contains(token)) result.add(token);
            }
        }
        return result;
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String normalizedDeadlineDays(String value) {
        Set<Integer> days = deadlineDays(value);
        return days.isEmpty() ? "7,3,1" : days.stream().sorted(Comparator.reverseOrder())
                .map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }
    private Set<Integer> deadlineDays(String value) {
        Set<Integer> result = new LinkedHashSet<>();
        if (value != null) for (String item : value.split(",")) {
            try {
                int day = Integer.parseInt(item.trim());
                if (day >= 0 && day <= 30) result.add(day);
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }
    private String shorten(String value, int max) { return value.length() <= max ? value : value.substring(0, max) + "…"; }
}
