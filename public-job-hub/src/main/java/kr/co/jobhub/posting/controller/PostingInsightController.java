package kr.co.jobhub.posting.controller;

import kr.co.jobhub.posting.domain.*;
import kr.co.jobhub.posting.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** 현재 공고와 과거 공고를 연결하고 기관별 누적 채용 통계를 제공한다. */
@RestController
@RequestMapping("/api/postings")
public class PostingInsightController {
    public record SimilarItem(Long id, String title, LocalDate postedAt, LocalDate deadline,
                              String employmentType, Integer headcount, List<String> categories,
                              boolean sameCategory, String sourceUrl) {}
    public record SimilarView(String organization, List<SimilarItem> items) {}
    public record OrganizationStats(String organization, long totalPostings, long regularPostings,
                                    Integer totalHeadcount, Long averageHeadcount,
                                    List<String> frequentCategories, List<Integer> frequentMonths,
                                    Long averageCycleDays) {}

    private final JobPostingRepository postings;
    private final RecruitmentPositionRepository positions;

    public PostingInsightController(JobPostingRepository postings, RecruitmentPositionRepository positions) {
        this.postings = postings;
        this.positions = positions;
    }

    @GetMapping("/{id}/similar")
    public SimilarView similar(@PathVariable Long id) {
        JobPosting current = posting(id);
        Set<String> currentCategories = categories(id);
        List<SimilarItem> items = postings.findAll().stream()
                .filter(value -> !value.id.equals(id) && sameOrganization(current.organization, value.organization))
                .sorted(Comparator.comparing((JobPosting value) -> value.postedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(value -> {
                    Set<String> categories = categories(value.id);
                    boolean sameCategory = !Collections.disjoint(currentCategories, categories);
                    Integer headcount = positions.findByPostingIdOrderById(value.id).stream()
                            .map(row -> row.headcount).filter(Objects::nonNull).reduce(0, Integer::sum);
                    return new SimilarItem(value.id, value.title, value.postedAt, value.deadline,
                            value.employmentType, headcount, categories.stream().sorted().toList(),
                            sameCategory, value.sourceUrl);
                })
                .sorted(Comparator.comparing(SimilarItem::sameCategory).reversed()
                        .thenComparing(SimilarItem::postedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8).toList();
        return new SimilarView(current.organization, items);
    }

    @GetMapping("/{id}/organization-stats")
    public OrganizationStats stats(@PathVariable Long id) {
        JobPosting current = posting(id);
        List<JobPosting> values = postings.findAll().stream()
                .filter(value -> sameOrganization(current.organization, value.organization)).toList();
        List<Integer> headcounts = values.stream().map(value -> positions.findByPostingIdOrderById(value.id).stream()
                        .map(row -> row.headcount).filter(Objects::nonNull).reduce(0, Integer::sum))
                .filter(value -> value > 0).toList();
        Map<String, Long> categoryCounts = values.stream().flatMap(value -> categories(value.id).stream())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        Map<Integer, Long> monthCounts = values.stream().map(value -> value.postedAt)
                .filter(Objects::nonNull).collect(Collectors.groupingBy(LocalDate::getMonthValue, Collectors.counting()));
        List<LocalDate> dates = values.stream().map(value -> value.postedAt).filter(Objects::nonNull).sorted().toList();
        List<Long> cycles = new ArrayList<>();
        for (int index = 1; index < dates.size(); index++) cycles.add(ChronoUnit.DAYS.between(dates.get(index - 1), dates.get(index)));
        return new OrganizationStats(current.organization, values.size(),
                values.stream().filter(value -> value.employmentType != null && value.employmentType.contains("정규직")).count(),
                headcounts.stream().reduce(0, Integer::sum),
                headcounts.isEmpty() ? null : Math.round(headcounts.stream().mapToInt(Integer::intValue).average().orElse(0)),
                top(categoryCounts), topMonths(monthCounts),
                cycles.isEmpty() ? null : Math.round(cycles.stream().mapToLong(Long::longValue).average().orElse(0)));
    }

    private JobPosting posting(Long id) {
        return postings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private Set<String> categories(Long id) {
        return positions.findByPostingIdOrderById(id).stream().map(value -> value.standardCategory)
                .filter(Objects::nonNull).flatMap(value -> Arrays.stream(value.split("[,，]")))
                .map(String::trim).filter(value -> !value.isBlank()).collect(Collectors.toSet());
    }
    private boolean sameOrganization(String left, String right) { return normalize(left).equals(normalize(right)); }
    private String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.KOREA).replaceAll("[^가-힣a-z0-9]", ""); }
    private List<String> top(Map<String, Long> values) { return values.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5).map(Map.Entry::getKey).toList(); }
    private List<Integer> topMonths(Map<Integer, Long> values) { return values.entrySet().stream().sorted(Map.Entry.<Integer, Long>comparingByValue().reversed()).limit(3).map(Map.Entry::getKey).toList(); }
}
