package kr.co.jobhub.career.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.FavoriteOrganization;
import kr.co.jobhub.career.repository.FavoriteOrganizationRepository;
import kr.co.jobhub.posting.domain.JobPosting;
import kr.co.jobhub.posting.repository.JobPostingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.*;
import java.util.*;

/** 관심 기관 등록, 명칭 통합, 신규 채용공고 모아보기를 제공한다. */
@RestController
@RequestMapping("/api/tools/favorite-organizations")
public class FavoriteOrganizationController {
    public record OrganizationRequest(@NotBlank @Size(max = 200) String organization) {}
    public record OrganizationOption(String organization, long postings) {}
    public record FavoriteView(Long id, String organization, List<String> aliases,
                               long openPostings, long newPostings, LocalDate nearestDeadline) {}
    public record FavoritePosting(Long id, String title, String organization, String region,
                                  LocalDate postedAt, LocalDate deadline, String sourceUrl, boolean fresh) {}
    public record ToggleResult(boolean followed) {}

    private final FavoriteOrganizationRepository favorites;
    private final JobPostingRepository postings;
    private final AppUserRepository users;

    public FavoriteOrganizationController(FavoriteOrganizationRepository favorites,
                                          JobPostingRepository postings, AppUserRepository users) {
        this.favorites = favorites;
        this.postings = postings;
        this.users = users;
    }

    @GetMapping("/search")
    public List<OrganizationOption> search(@RequestParam(defaultValue = "") String q) {
        String query = q.trim().toLowerCase(Locale.KOREA);
        return postings.findAll().stream()
                .map(value -> value.organization).filter(Objects::nonNull)
                .filter(value -> query.isBlank() || value.toLowerCase(Locale.KOREA).contains(query))
                .collect(java.util.stream.Collectors.groupingBy(value -> value, java.util.stream.Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(20).map(entry -> new OrganizationOption(entry.getKey(), entry.getValue())).toList();
    }

    @GetMapping
    public List<FavoriteView> list(Principal principal) {
        AppUser user = user(principal);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<JobPosting> all = postings.findAll();
        return favorites.findByUserIdOrderByOrganizationName(user.id).stream().map(favorite -> {
            List<JobPosting> matched = all.stream()
                    .filter(value -> favorite.normalizedName.equals(normalize(value.organization))).toList();
            List<JobPosting> open = matched.stream()
                    .filter(value -> value.deadline == null || !value.deadline.isBefore(today)).toList();
            long fresh = open.stream().filter(value -> favorite.lastViewedAt == null
                    || value.updatedAt.isAfter(favorite.lastViewedAt)).count();
            List<String> aliases = matched.stream().map(value -> value.organization)
                    .filter(Objects::nonNull).distinct().sorted().toList();
            LocalDate nearest = open.stream().map(value -> value.deadline).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
            return new FavoriteView(favorite.id, favorite.organizationName, aliases,
                    open.size(), fresh, nearest);
        }).toList();
    }

    @PostMapping("/toggle")
    public ToggleResult toggle(@Valid @RequestBody OrganizationRequest request, Principal principal) {
        AppUser user = user(principal);
        String name = request.organization().trim();
        String normalized = normalize(name);
        if (normalized.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기관명을 확인해 주세요.");
        Optional<FavoriteOrganization> existing = favorites.findByUserIdAndNormalizedName(user.id, normalized);
        if (existing.isPresent()) {
            favorites.delete(existing.get());
            return new ToggleResult(false);
        }
        boolean known = postings.findAll().stream().anyMatch(value -> normalized.equals(normalize(value.organization)));
        if (!known) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수집된 공고에서 기관을 찾을 수 없습니다.");
        FavoriteOrganization value = new FavoriteOrganization();
        value.user = user;
        value.organizationName = name;
        value.normalizedName = normalized;
        value.lastViewedAt = Instant.now();
        favorites.save(value);
        return new ToggleResult(true);
    }

    @GetMapping("/postings")
    public List<FavoritePosting> favoritePostings(Principal principal) {
        AppUser user = user(principal);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Map<String, FavoriteOrganization> followed = new HashMap<>();
        favorites.findByUserIdOrderByOrganizationName(user.id)
                .forEach(value -> followed.put(value.normalizedName, value));
        return postings.findAll().stream()
                .filter(value -> followed.containsKey(normalize(value.organization)))
                .filter(value -> value.deadline == null || !value.deadline.isBefore(today))
                .sorted(Comparator.comparing((JobPosting value) -> value.deadline,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(value -> value.updatedAt, Comparator.reverseOrder()))
                .limit(100).map(value -> {
                    FavoriteOrganization favorite = followed.get(normalize(value.organization));
                    boolean fresh = favorite.lastViewedAt == null || value.updatedAt.isAfter(favorite.lastViewedAt);
                    return new FavoritePosting(value.id, value.title, value.organization, value.region,
                            value.postedAt, value.deadline, value.sourceUrl, fresh);
                }).toList();
    }

    @PostMapping("/read")
    public void read(Principal principal) {
        AppUser user = user(principal);
        List<FavoriteOrganization> values = favorites.findByUserIdOrderByOrganizationName(user.id);
        Instant now = Instant.now();
        values.forEach(value -> value.lastViewedAt = now);
        favorites.saveAll(values);
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** 법인 표기와 공백·문장부호 차이를 제거해 같은 기관의 명칭 변형을 묶는다. */
    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.KOREA)
                .replaceAll("^(주식회사|재단법인|사단법인|학교법인|의료법인|\\(주\\)|\\(재\\)|\\(사\\)|㈜)", "")
                .replaceAll("[^가-힣a-z0-9]", "");
    }
}
