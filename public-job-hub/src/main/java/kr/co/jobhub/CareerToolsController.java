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
            "SAVED", "PREPARING", "SUBMITTED", "WRITTEN_TEST", "INTERVIEW", "PASSED", "REJECTED");

    public record ApplicationRequest(String stage, LocalDate nextStepDate, @Size(max = 2000) String memo) {}
    public record ApplicationState(String stage, LocalDate nextStepDate, String memo) {}
    public record PreferenceRequest(@Size(max = 500) String keywords, @Size(max = 500) String regions,
                                    @Size(max = 100) String mobilityTypes, boolean enabled) {}
    public record Preference(String keywords, String regions, String mobilityTypes,
                             boolean enabled, Instant lastViewedAt) {}
    public record AlertItem(Long id, String title, String organization, String region,
                            LocalDate deadline, String mobilityType, String sourceUrl, boolean fresh) {}
    public record MatchResult(List<String> matchedCertifications, List<String> requirementEvidence,
                              String summary) {}
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

    @GetMapping("/preferences")
    public Preference preference(Principal principal) {
        AppUser user = user(principal);
        return preferences.findByUserId(user.id).map(this::preference)
                .orElseGet(() -> new Preference("", "", "", true, null));
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
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        return postings.findAll().stream()
                .filter(p -> p.deadline == null || !p.deadline.isBefore(today))
                .filter(p -> keywords.isEmpty() || keywords.stream().anyMatch(k -> contains(p.title, k) || contains(p.organization, k)))
                .filter(p -> regions.isEmpty() || regions.stream().anyMatch(r -> contains(p.region, r)))
                .filter(p -> mobility.isEmpty() || mobility.contains(p.mobilityType))
                .sorted(Comparator.comparing((JobPosting p) -> p.deadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(30)
                .map(p -> new AlertItem(p.id, p.title, p.organization, p.region, p.deadline, p.mobilityType,
                        p.sourceUrl,
                        pref.lastViewedAt == null || p.updatedAt.isAfter(pref.lastViewedAt)))
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
                .filter(Objects::nonNull).distinct().sorted().toList();
        return new FilterOptions(regions, List.of("ROTATIONAL", "FIXED", "UNKNOWN"), categories);
    }

    @GetMapping("/postings/{id}/match")
    public MatchResult match(@PathVariable Long id, Principal principal) {
        AppUser user = user(principal);
        JobPosting posting = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<String> certificates = profileCertificates(user.id);
        try {
            Document doc = Jsoup.connect(posting.sourceUrl).userAgent("PublicJobHub/0.4")
                    .maxBodySize(3_000_000).timeout(12000).get();
            String text = doc.text();
            List<String> matched = certificates.stream().filter(name -> contains(text, name)).toList();
            List<String> evidence = new ArrayList<>();
            for (Element heading : doc.select("#contentRV h4, main h2, main h3, article h2, article h3")) {
                String label = heading.text();
                if (label.contains("자격") || label.contains("우대") || label.contains("전형")) {
                    Element next = heading.nextElementSibling();
                    if (next != null) evidence.add(label + ": " + shorten(next.text(), 500));
                }
                if (evidence.size() == 6) break;
            }
            String summary = matched.isEmpty()
                    ? "보유 자격증과 공고문에서 직접 일치하는 명칭을 찾지 못했습니다. 원문 확인이 필요합니다."
                    : "보유 자격증 중 " + matched.size() + "개가 공고문에 언급되어 있습니다.";
            return new MatchResult(matched, evidence, summary);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "공고문을 분석하지 못했습니다.");
        }
    }

    private List<String> profileCertificates(Long userId) {
        String raw = profiles.findByUserId(userId).map(p -> p.certifications).orElse("[]");
        try {
            List<String> out = new ArrayList<>();
            for (JsonNode item : json.readTree(raw)) {
                String name = item.path("name").asText().trim();
                if (!name.isBlank()) out.add(name);
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private Preference preference(JobAlertPreference value) {
        return new Preference(clean(value.keywords), clean(value.regions), clean(value.mobilityTypes),
                value.enabled, value.lastViewedAt);
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
    private String shorten(String value, int max) { return value.length() <= max ? value : value.substring(0, max) + "…"; }
}
