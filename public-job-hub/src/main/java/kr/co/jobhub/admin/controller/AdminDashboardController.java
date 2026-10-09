package kr.co.jobhub.admin.controller;

import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.collection.service.CrawlService;
import kr.co.jobhub.collection.service.PublicRecruitmentApiService;
import kr.co.jobhub.community.domain.*;
import kr.co.jobhub.community.repository.*;
import kr.co.jobhub.posting.domain.*;
import kr.co.jobhub.posting.repository.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** 관리자가 서비스 운영 상태를 한 화면에서 확인할 요약 지표를 제공한다. */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    public record SourceStat(String source, long total, long open, Instant lastUpdatedAt) {}
    public record Activity(String type, String title, String detail, Instant occurredAt) {}
    public record DashboardView(long totalPostings, long openPostings, long collectedToday,
                                long qualityAttention, long totalUsers, long newUsersToday,
                                long waitingInquiries, long answeredInquiries, long notices,
                                List<SourceStat> sources, List<Activity> recentActivities,
                                CrawlService.Status crawlStatus,
                                PublicRecruitmentApiService.Status publicApiStatus,
                                Instant checkedAt) {}

    private final JobPostingRepository postings;
    private final RecruitmentPositionRepository positions;
    private final AppUserRepository users;
    private final InquiryRepository inquiries;
    private final NoticeRepository notices;
    private final CrawlService crawler;
    private final PublicRecruitmentApiService publicApi;

    public AdminDashboardController(JobPostingRepository postings, RecruitmentPositionRepository positions,
                                    AppUserRepository users, InquiryRepository inquiries,
                                    NoticeRepository notices, CrawlService crawler,
                                    PublicRecruitmentApiService publicApi) {
        this.postings = postings;
        this.positions = positions;
        this.users = users;
        this.inquiries = inquiries;
        this.notices = notices;
        this.crawler = crawler;
        this.publicApi = publicApi;
    }

    @GetMapping
    public DashboardView dashboard() {
        List<JobPosting> allPostings = postings.findAll();
        List<AppUser> allUsers = users.findAll();
        List<Inquiry> allInquiries = inquiries.findAllByOrderByCreatedAtDesc();
        Set<Long> withPositions = positions.findAll().stream()
                .map(value -> value.posting.id).collect(Collectors.toSet());
        Instant today = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        long attention = allPostings.stream().filter(value -> value.deadline == null
                || value.region == null || value.region.isBlank()
                || value.mobilityType == null || "UNKNOWN".equals(value.mobilityType)
                || !withPositions.contains(value.id)).count();

        List<SourceStat> sourceStats = allPostings.stream()
                .collect(Collectors.groupingBy(value -> value.source == null ? "미분류" : value.source))
                .entrySet().stream().map(entry -> new SourceStat(
                        entry.getKey(), entry.getValue().size(),
                        entry.getValue().stream().filter(value -> value.open).count(),
                        entry.getValue().stream().map(value -> value.updatedAt)
                                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null)))
                .sorted(Comparator.comparingLong(SourceStat::total).reversed()).toList();

        List<Activity> activityCandidates = new ArrayList<>();
        notices.findAllByOrderByPinnedDescCreatedAtDesc().stream().limit(10)
                .forEach(value -> activityCandidates.add(new Activity("NOTICE", "공지 등록", value.title, value.createdAt)));
        allInquiries.stream().filter(value -> value.answeredAt != null).limit(10)
                .forEach(value -> activityCandidates.add(new Activity("ANSWER", "문의 답변", value.title, value.answeredAt)));
        List<Activity> activities = activityCandidates.stream()
                .sorted(Comparator.comparing(Activity::occurredAt).reversed())
                .limit(10).toList();

        return new DashboardView(
                allPostings.size(), allPostings.stream().filter(value -> value.open).count(),
                allPostings.stream().filter(value -> value.firstSeenAt != null
                        && !value.firstSeenAt.isBefore(today)).count(),
                attention, allUsers.size(),
                allUsers.stream().filter(value -> value.createdAt != null
                        && !value.createdAt.isBefore(today)).count(),
                allInquiries.stream().filter(value -> "WAITING".equals(value.status)).count(),
                allInquiries.stream().filter(value -> "ANSWERED".equals(value.status)).count(),
                notices.count(), sourceStats, activities, crawler.status(), publicApi.status(), Instant.now());
    }
}
