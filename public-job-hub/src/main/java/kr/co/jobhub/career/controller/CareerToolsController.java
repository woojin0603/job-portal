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
    public record MatchResult(List<String> matchedCertifications, List<String> requirementEvidence,
                              List<String> strengths, List<String> gaps, int score, String summary,
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
        positions.findByPostingIdOrderById(posting.id).forEach(position -> {
            sourceText.append(' ').append(clean(position.standardCategory))
                    .append(' ').append(clean(position.originalName))
                    .append(' ').append(clean(position.requirements));
            if (position.requirements != null && !position.requirements.isBlank() && evidence.size() < 4) {
                evidence.add(shorten(position.requirements.replaceAll("\\s+", " "), 500));
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
        Set<String> sourceTokens = meaningfulTokens(List.of(text));
        Set<String> profileTokens = meaningfulTokens(java.util.stream.Stream.of(
                        signals.certificates(), signals.majors(), signals.experiences())
                .flatMap(Collection::stream).toList());
        List<String> matched = signals.certificates().stream()
                .filter(name -> normalizedContains(text, name)).distinct().toList();
        List<String> strengths = new ArrayList<>();
        matched.forEach(name -> strengths.add("보유 자격증 일치: " + name));
        List<String> matchedMajors = signals.majors().stream()
                .filter(value -> normalizedContains(text, value)
                        || meaningfulTokens(List.of(value)).stream().anyMatch(sourceTokens::contains))
                .distinct().limit(3).toList();
        matchedMajors.stream()
                .forEach(value -> strengths.add("전공 관련성: " + value));
        List<String> experienceMatches = signals.experiences().stream()
                .filter(value -> meaningfulTokens(List.of(value)).stream().anyMatch(sourceTokens::contains))
                .distinct().limit(3).toList();
        experienceMatches.forEach(value -> strengths.add("경력·활동 핵심어 관련: " + shorten(value, 80)));
        boolean preferredMatch = !signals.preferredJobCategory().isBlank()
                && normalizedContains(text, signals.preferredJobCategory());
        if (preferredMatch) strengths.add("희망직무 일치: " + signals.preferredJobCategory());
        List<String> gaps = new ArrayList<>();
        for (String line : evidence) {
            if (gaps.size() == 4) break;
            if ((line.contains("필수") || line.contains("자격") || line.contains("면허") || line.contains("우대"))
                    && profileTokens.stream().noneMatch(token -> normalizedContains(line, token))) {
                gaps.add("원문 확인 필요: " + shorten(line, 180));
            }
        }
        if (signals.certificates().isEmpty()) gaps.add("등록된 자격증이 없어 자격요건 비교가 제한됩니다.");
        int score = Math.min(100, Math.min(50, matched.size() * 25)
                + Math.min(24, matchedMajors.size() * 12)
                + Math.min(20, experienceMatches.size() * 7)
                + (preferredMatch ? 15 : 0));
        String summary = strengths.isEmpty()
                ? "등록 정보와 공고문에서 직접 확인되는 일치 근거가 없습니다. 점수는 합격 가능성이 아니므로 원문 요건을 확인해 주세요."
                : "공고문에서 " + strengths.size() + "개의 관련 근거를 확인했습니다. 점수는 자격 충족이나 합격 가능성을 보장하지 않습니다.";
        return new MatchResult(matched, evidence, strengths, gaps, score, summary,
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
                reasons.add("경력·활동: " + String.join(", ", matches));
            }
        }
        if (!signals.preferredJobCategory().isBlank()
                && normalizedContains(source, signals.preferredJobCategory())) {
            score += 20;
            reasons.add("희망직무 " + signals.preferredJobCategory());
        }
        String profileText = String.join(" ", signals.certificates()) + " "
                + String.join(" ", signals.majors()) + " " + String.join(" ", signals.experiences())
                + " " + signals.preferredJobCategory();
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
                                  List<String> experiences, String preferredJobCategory) {}

    private ProfileSignals profileSignals(AppUser user) {
        try {
            UserProfile profile = profiles.findByUserId(user.id).orElse(null);
            String preferred = clean(user.preferredJobCategory);
            if (profile == null) return new ProfileSignals(List.of(), List.of(), List.of(), preferred);
            List<String> certificates = new ArrayList<>();
            for (JsonNode item : json.readTree(profile.certifications == null ? "[]" : profile.certifications)) {
                String name = item.path("name").asText().trim();
                if (!name.isBlank()) certificates.add(name);
            }
            List<String> majors = new ArrayList<>();
            for (JsonNode item : json.readTree(profile.degree == null ? "[]" : profile.degree)) {
                String major = item.path("major").asText().trim();
                if (!major.isBlank()) majors.add(major);
            }
            List<String> experiences = new ArrayList<>();
            for (String raw : Arrays.asList(profile.careerHistory, profile.activities)) {
                if (raw == null || raw.isBlank()) continue;
                for (JsonNode item : json.readTree(raw)) {
                    for (String field : List.of("companyName", "position", "duties", "name", "description")) {
                        String value = item.path(field).asText().trim();
                        if (value.length() >= 2) experiences.add(value);
                    }
                }
            }
            return new ProfileSignals(certificates.stream().distinct().toList(),
                    majors.stream().distinct().toList(), experiences.stream().distinct().toList(), preferred);
        } catch (Exception e) {
            return new ProfileSignals(List.of(), List.of(), List.of(), clean(user.preferredJobCategory));
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
