package kr.co.jobhub.admin.controller;

import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.Scrap;
import kr.co.jobhub.career.repository.ScrapRepository;
import kr.co.jobhub.community.domain.Inquiry;
import kr.co.jobhub.community.repository.InquiryRepository;
import kr.co.jobhub.posting.domain.JobPosting;
import kr.co.jobhub.posting.repository.JobPostingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 별도 통계 테이블 없이 최근 운영 데이터를 관리자용 추이로 집계한다. */
@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {
    public record DailyPoint(LocalDate date, long postings, long users, long scraps, long inquiries) {}
    public record SourceShare(String source, long total, long open) {}
    public record StatisticsView(int periodDays, List<DailyPoint> daily, List<SourceShare> sources,
                                 long collectedInPeriod, long joinedInPeriod, long scrapsInPeriod,
                                 long inquiriesInPeriod, double openRate, double inquiryAnswerRate,
                                 Instant checkedAt) {}

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int PERIOD_DAYS = 14;

    private final JobPostingRepository postings;
    private final AppUserRepository users;
    private final ScrapRepository scraps;
    private final InquiryRepository inquiries;

    public AdminStatisticsController(JobPostingRepository postings, AppUserRepository users,
                                     ScrapRepository scraps, InquiryRepository inquiries) {
        this.postings = postings;
        this.users = users;
        this.scraps = scraps;
        this.inquiries = inquiries;
    }

    @GetMapping
    public StatisticsView statistics() {
        LocalDate today = LocalDate.now(SEOUL);
        LocalDate start = today.minusDays(PERIOD_DAYS - 1L);
        List<JobPosting> allPostings = postings.findAll();
        List<AppUser> allUsers = users.findAll();
        List<Scrap> allScraps = scraps.findAll();
        List<Inquiry> allInquiries = inquiries.findAllByOrderByCreatedAtDesc();

        Map<LocalDate, long[]> daily = new LinkedHashMap<>();
        for (int offset = 0; offset < PERIOD_DAYS; offset++) {
            daily.put(start.plusDays(offset), new long[4]);
        }
        allPostings.forEach(value -> increment(daily, value.firstSeenAt, 0));
        allUsers.forEach(value -> increment(daily, value.createdAt, 1));
        allScraps.forEach(value -> increment(daily, value.scrappedAt, 2));
        allInquiries.forEach(value -> increment(daily, value.createdAt, 3));

        List<DailyPoint> points = new ArrayList<>();
        daily.forEach((date, counts) -> points.add(
                new DailyPoint(date, counts[0], counts[1], counts[2], counts[3])));
        List<SourceShare> sourceShares = allPostings.stream()
                .collect(Collectors.groupingBy(value ->
                        value.source == null || value.source.isBlank() ? "미분류" : value.source))
                .entrySet().stream()
                .map(entry -> new SourceShare(entry.getKey(), entry.getValue().size(),
                        entry.getValue().stream().filter(value -> value.open).count()))
                .sorted(Comparator.comparingLong(SourceShare::total).reversed())
                .limit(8).toList();

        long answered = allInquiries.stream().filter(value -> "ANSWERED".equals(value.status)).count();
        return new StatisticsView(PERIOD_DAYS, points, sourceShares,
                points.stream().mapToLong(DailyPoint::postings).sum(),
                points.stream().mapToLong(DailyPoint::users).sum(),
                points.stream().mapToLong(DailyPoint::scraps).sum(),
                points.stream().mapToLong(DailyPoint::inquiries).sum(),
                percentage(allPostings.stream().filter(value -> value.open).count(), allPostings.size()),
                percentage(answered, allInquiries.size()), Instant.now());
    }

    private void increment(Map<LocalDate, long[]> daily, Instant instant, int index) {
        if (instant == null) return;
        long[] counts = daily.get(instant.atZone(SEOUL).toLocalDate());
        if (counts != null) counts[index]++;
    }

    private double percentage(long value, long total) {
        return total == 0 ? 0 : Math.round(value * 1000.0 / total) / 10.0;
    }
}
