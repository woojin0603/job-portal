package kr.co.jobhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.model.*;
import kr.co.jobhub.repo.*;
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

    public record ApplicationRequest(String stage, LocalDate nextStepDate, @Size(max = 2000) String memo) {}
    public record ApplicationState(String stage, LocalDate nextStepDate, String memo) {}
    public record ApplicationItem(Long id, String title, String organization, LocalDate deadline,
                                  String sourceUrl, String stage, LocalDate nextStepDate, String memo) {}
    public record PreferenceRequest(@Size(max = 500) String keywords, @Size(max = 500) String regions,
                                    @Size(max = 100) String mobilityTypes, boolean enabled,
                                    boolean newPostingAlerts, boolean deadlineAlerts,
                                    @Size(max = 30) String deadlineDays) {}
    public record Preference(String keywords, String regions, String mobilityTypes,
                             boolean enabled, boolean newPostingAlerts, boolean deadlineAlerts,
                             String deadlineDays, Instant lastViewedAt) {}
    public record AlertItem(Long id, String title, String organization, String region,
                            LocalDate deadline, String mobilityType, String sourceUrl, boolean fresh,
                            Long remainingDays, String reason) {}
    public record MatchResult(List<String> matchedCertifications, List<String> requirementEvidence,
                              List<String> strengths, List<String> gaps, int score, String summary) {}
    public record FilterOptions(List<String> regions, List<String> mobilityTypes,
                                List<String> jobCategories) {}

    private final AppUserRepository users;
    private final JobPostingRepository postings;
    private final ScrapRepository scraps;
    private final UserProfileRepository profiles;
    private final JobAlertPreferenceRepository preferences;
    private final RecruitmentPositionRepository positions;
    private final ObjectMapper json;

    public CareerToolsController(AppUserRepository users, JobPostingRepository postings,
                                 ScrapRepository scraps, UserProfileRepository profiles,
                                 JobAlertPreferenceRepository preferences,
                                 RecruitmentPositionRepository positions, ObjectMapper json) {
        this.users = users;
        this.postings = postings;
        this.scraps = scraps;
        this.profiles = profiles;
        this.preferences = preferences;
        this.positions = positions;
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
        return scraps.findByUserId(user.id).stream()
                .sorted(Comparator
                        .comparing((Scrap scrap) -> scrap.nextStepDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(scrap -> scrap.posting.deadline,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(scrap -> new ApplicationItem(
                        scrap.posting.id,
                        scrap.posting.title,
                        scrap.posting.organization,
                        scrap.posting.deadline,
                        scrap.posting.sourceUrl,
                        scrap.stage == null ? "SAVED" : scrap.stage,
                        scrap.nextStepDate,
                        scrap.memo == null ? "" : scrap.memo))
                .toList();
    }

    @GetMapping("/preferences")
    public Preference preference(Principal principal) {
        AppUser user = user(principal);
        return preferences.findByUserId(user.id).map(this::preference)
                .orElseGet(() -> new Preference("", "", "", true, true, true, "7,3,1", null));
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
        value.updatedAt = Instant.now();
        return preference(preferences.save(value));
    }

    @GetMapping("/alerts")
    public List<AlertItem> alerts(Principal principal) {
        AppUser user = user(principal);
        JobAlertPreference pref = preferences.findByUserId(user.id).orElse(null);
        if (pref == null || !pref.enabled) return List.of();
        Set<String> keywords = csv(pref.keywords);
        Set<String> regions = csv(pref.regions);
        Set<String> mobility = csv(pref.mobilityTypes);
        Set<Integer> deadlineDays = deadlineDays(pref.deadlineDays);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        return postings.findAll().stream()
                .filter(p -> p.deadline == null || !p.deadline.isBefore(today))
                .filter(p -> keywords.isEmpty() || keywords.stream().anyMatch(k -> contains(p.title, k) || contains(p.organization, k)))
                .filter(p -> regions.isEmpty() || regions.stream().anyMatch(r -> contains(p.region, r)))
                .filter(p -> mobility.isEmpty() || mobility.contains(p.mobilityType))
                .filter(p -> {
                    boolean fresh = pref.lastViewedAt == null || p.updatedAt.isAfter(pref.lastViewedAt);
                    long remaining = p.deadline == null ? Long.MIN_VALUE
                            : java.time.temporal.ChronoUnit.DAYS.between(today, p.deadline);
                    return pref.newPostingAlerts && fresh
                            || pref.deadlineAlerts && deadlineDays.contains((int) remaining);
                })
                .sorted(Comparator.comparing((JobPosting p) -> p.deadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(30)
                .map(p -> {
                    boolean fresh = pref.lastViewedAt == null || p.updatedAt.isAfter(pref.lastViewedAt);
                    Long remaining = p.deadline == null ? null
                            : java.time.temporal.ChronoUnit.DAYS.between(today, p.deadline);
                    boolean deadlineDue = remaining != null && deadlineDays.contains(remaining.intValue());
                    String reason = deadlineDue ? remaining == 0 ? "오늘 마감" : "마감 " + remaining + "일 전"
                            : "새로운 공고";
                    return new AlertItem(p.id, p.title, p.organization, p.region, p.deadline, p.mobilityType,
                            p.sourceUrl, fresh, remaining, reason);
                })
                .toList();
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
        List<String> categories = positions.findAll().stream().map(p -> p.standardCategory)
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split("[,，]")))
                .map(String::trim).filter(value -> !value.isBlank())
                .distinct().sorted().toList();
        return new FilterOptions(regions, List.of("ROTATIONAL", "FIXED", "UNKNOWN"), categories);
    }

    @GetMapping("/postings/{id}/match")
    public MatchResult match(@PathVariable Long id, Principal principal) {
        AppUser user = user(principal);
        JobPosting posting = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ProfileSignals signals = profileSignals(user.id);
        List<String> evidence = new ArrayList<>();
        StringBuilder sourceText = new StringBuilder(posting.title).append(' ')
                .append(Optional.ofNullable(posting.organization).orElse(""));
        positions.findByPostingIdOrderById(posting.id).stream()
                .map(position -> position.requirements)
                .filter(Objects::nonNull).filter(value -> !value.isBlank())
                .forEach(value -> {
                    sourceText.append(' ').append(value);
                    if (evidence.size() < 4) evidence.add(shorten(value.replaceAll("\\s+", " "), 500));
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
        List<String> matched = signals.certificates().stream().filter(name -> contains(text, name)).toList();
        List<String> strengths = new ArrayList<>();
        matched.forEach(name -> strengths.add("보유 자격증 일치: " + name));
        signals.majors().stream().filter(value -> contains(text, value)).limit(3)
                .forEach(value -> strengths.add("전공 관련성: " + value));
        signals.experiences().stream().filter(value -> contains(text, value)).limit(3)
                .forEach(value -> strengths.add("경력·활동 관련성: " + shorten(value, 80)));
        List<String> gaps = new ArrayList<>();
        for (String line : evidence) {
            if (gaps.size() == 4) break;
            if ((line.contains("필수") || line.contains("자격") || line.contains("면허"))
                    && matched.stream().noneMatch(name -> contains(line, name))) {
                gaps.add(shorten(line, 180));
            }
        }
        if (signals.certificates().isEmpty()) gaps.add("등록된 자격증이 없어 자격요건 비교가 제한됩니다.");
        int score = Math.min(95, 35 + matched.size() * 20
                + Math.min(20, Math.max(0, strengths.size() - matched.size()) * 10));
        String summary = strengths.isEmpty()
                ? "직접 일치하는 역량을 찾지 못했습니다. 부족하다는 뜻은 아니며 원문 확인이 필요합니다."
                : "등록한 역량 중 " + strengths.size() + "개 항목에서 공고와의 관련성을 찾았습니다.";
        return new MatchResult(matched, evidence, strengths, gaps, score, summary);
    }

    private record ProfileSignals(List<String> certificates, List<String> majors, List<String> experiences) {}

    private ProfileSignals profileSignals(Long userId) {
        try {
            UserProfile profile = profiles.findByUserId(userId).orElse(null);
            if (profile == null) return new ProfileSignals(List.of(), List.of(), List.of());
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
            return new ProfileSignals(certificates, majors, experiences);
        } catch (Exception e) {
            return new ProfileSignals(List.of(), List.of(), List.of());
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
                normalizedDeadlineDays(value.deadlineDays), value.lastViewedAt);
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
