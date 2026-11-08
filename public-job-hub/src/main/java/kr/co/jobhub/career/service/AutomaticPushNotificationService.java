package kr.co.jobhub.career.service;

import kr.co.jobhub.admin.domain.ServicePopup;
import kr.co.jobhub.admin.repository.ServicePopupRepository;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.*;
import kr.co.jobhub.career.repository.*;
import kr.co.jobhub.posting.domain.JobPosting;
import kr.co.jobhub.posting.domain.PostingChange;
import kr.co.jobhub.posting.repository.JobPostingRepository;
import kr.co.jobhub.posting.repository.PostingChangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** 스크랩·관심기관·공고 변경·관리자 공지를 실제 FCM 푸시로 전달한다. */
@Service
public class AutomaticPushNotificationService {
    private static final Logger log = LoggerFactory.getLogger(AutomaticPushNotificationService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MAX_PER_USER_PER_RUN = 12;

    private final PushNotificationService notifications;
    private final PushSubscriptionRepository subscriptions;
    private final PushDeliveryReceiptRepository receipts;
    private final JobAlertPreferenceRepository preferences;
    private final ScrapRepository scraps;
    private final FavoriteOrganizationRepository favorites;
    private final JobPostingRepository postings;
    private final PostingChangeRepository changes;
    private final ServicePopupRepository popups;
    private final AppUserRepository users;
    private final boolean automaticEnabled;

    public record ManualTestResult(boolean configured, boolean delivered, boolean duplicateBlocked,
                                   String title, String detail) {}

    public AutomaticPushNotificationService(PushNotificationService notifications,
                                            PushSubscriptionRepository subscriptions,
                                            PushDeliveryReceiptRepository receipts,
                                            JobAlertPreferenceRepository preferences,
                                            ScrapRepository scraps,
                                            FavoriteOrganizationRepository favorites,
                                            JobPostingRepository postings,
                                            PostingChangeRepository changes,
                                            ServicePopupRepository popups,
                                            AppUserRepository users,
                                            @Value("${jobhub.push.automatic-enabled:true}") boolean automaticEnabled) {
        this.notifications = notifications;
        this.subscriptions = subscriptions;
        this.receipts = receipts;
        this.preferences = preferences;
        this.scraps = scraps;
        this.favorites = favorites;
        this.postings = postings;
        this.changes = changes;
        this.popups = popups;
        this.users = users;
        this.automaticEnabled = automaticEnabled;
    }

    @Scheduled(fixedDelayString = "${jobhub.push.automatic-delay-ms:300000}",
            initialDelayString = "${jobhub.push.automatic-initial-delay-ms:120000}")
    public void deliverAutomaticNotifications() {
        if (!automaticEnabled || !notifications.enabled()) return;
        List<Long> userIds = subscriptions.findDistinctEnabledUserIds();
        if (userIds.isEmpty()) return;

        Instant now = Instant.now();
        LocalDate today = LocalDate.now(SEOUL);
        Map<Long, JobAlertPreference> preferenceByUser = preferences.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(value -> value.user.getId(), value -> value));
        Map<Long, List<Scrap>> scrapsByUser = scraps.findWithUserAndPostingByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(value -> value.user.getId()));
        Map<Long, List<FavoriteOrganization>> favoritesByUser = favorites.findWithUserByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(value -> value.user.getId()));

        Set<Long> scrappedPostingIds = scrapsByUser.values().stream().flatMap(Collection::stream)
                .map(value -> value.posting.getId()).collect(Collectors.toSet());
        List<PostingChange> recentChanges = scrappedPostingIds.isEmpty() ? List.of()
                : changes.findTop200ByPosting_IdInAndDetectedAtAfterOrderByDetectedAtDesc(
                        scrappedPostingIds, now.minus(48, ChronoUnit.HOURS));
        List<JobPosting> recentPostings = postings.findByFirstSeenAtAfterOrderByFirstSeenAtDesc(
                now.minus(48, ChronoUnit.HOURS));
        List<ServicePopup> recentPopups = popups.findByActiveTrueAndUpdatedAtAfterOrderByUpdatedAtDesc(
                now.minus(7, ChronoUnit.DAYS));

        int delivered = 0;
        for (Long userId : userIds) {
            int remaining = MAX_PER_USER_PER_RUN;
            JobAlertPreference preference = preferenceByUser.get(userId);
            List<Scrap> userScraps = scrapsByUser.getOrDefault(userId, List.of());

            if (preference != null && preference.enabled && preference.deadlineAlerts) {
                Set<Integer> days = deadlineDays(preference.deadlineDays);
                for (Scrap scrap : userScraps) {
                    if (remaining <= 0) break;
                    JobPosting posting = scrap.posting;
                    if (posting.deadline == null || posting.deadline.isBefore(today)) continue;
                    long daysLeft = ChronoUnit.DAYS.between(today, posting.deadline);
                    if (!days.contains((int) daysLeft)) continue;
                    String timing = daysLeft == 0 ? "오늘 마감" : "마감 " + daysLeft + "일 전";
                    if (deliver(userId, "deadline:" + posting.id + ":" + posting.deadline + ":" + daysLeft,
                            "스크랩 공고 " + timing,
                            posting.organization + " · " + posting.title,
                            "/postings/" + posting.id, "스크랩 마감")) {
                        delivered++; remaining--;
                    }
                }
            }

            if (remaining > 0 && preference != null && preference.enabled
                    && Boolean.TRUE.equals(preference.postingChangeAlerts)) {
                Set<Long> ids = userScraps.stream().map(value -> value.posting.getId()).collect(Collectors.toSet());
                for (PostingChange change : recentChanges) {
                    if (remaining <= 0) break;
                    if (!ids.contains(change.posting.getId())) continue;
                    if (Boolean.TRUE.equals(preference.importantChangeOnly)
                            && !"IMPORTANT".equalsIgnoreCase(change.importance)) continue;
                    if (deliver(userId, "change:" + change.id,
                            "스크랩 공고 변경 안내",
                            change.posting.organization + " · " + change.fieldLabel + " 변경",
                            "/postings/" + change.posting.getId(), "공고 변경")) {
                        delivered++; remaining--;
                    }
                }
            }

            if (remaining > 0 && preference != null && preference.enabled && preference.newPostingAlerts) {
                List<FavoriteOrganization> followed = favoritesByUser.getOrDefault(userId, List.of());
                for (JobPosting posting : recentPostings) {
                    if (remaining <= 0) break;
                    FavoriteOrganization favorite = followed.stream()
                            .filter(value -> value.normalizedName.equals(normalize(posting.organization)))
                            .findFirst().orElse(null);
                    if (favorite == null || posting.firstSeenAt.isBefore(favorite.createdAt)) continue;
                    if (posting.deadline != null && posting.deadline.isBefore(today)) continue;
                    if (deliver(userId, "favorite:" + favorite.id + ":" + posting.id,
                            favorite.organizationName + " 신규 채용공고",
                            posting.title, "/postings/" + posting.id, "관심 기관 신규 공고")) {
                        delivered++; remaining--;
                    }
                }
            }

            for (ServicePopup popup : recentPopups) {
                if (remaining <= 0) break;
                if (deliver(userId, "popup:" + popup.id + ":" + popup.updatedAt.toEpochMilli(),
                        popup.title, summary(popup.content), "/", "중요 공지")) {
                    delivered++; remaining--;
                }
            }
        }
        if (delivered > 0) log.info("Automatic push delivery completed: {} notification(s)", delivered);
    }

    @Scheduled(cron = "0 40 4 * * *", zone = "Asia/Seoul")
    public void deleteOldReceipts() {
        long deleted = receipts.deleteByDeliveredAtBefore(Instant.now().minus(180, ChronoUnit.DAYS));
        if (deleted > 0) log.info("Deleted {} old push delivery receipt(s)", deleted);
    }

    /** 관리자 본인의 등록 기기로 운영 알림 형식과 중복 방지를 안전하게 점검한다. */
    public ManualTestResult deliverManualTest(Long userId, String scenario) {
        if (!notifications.enabled()) {
            return new ManualTestResult(false, false, false, "푸시 설정 확인 필요",
                    "Firebase 서버 설정이 활성화되지 않았습니다.");
        }
        String normalized = scenario == null ? "" : scenario.trim().toUpperCase(Locale.ROOT);
        String nonce = UUID.randomUUID().toString();
        String title;
        String body;
        String category;
        String link = "/mypage";
        switch (normalized) {
            case "SCRAP_CHANGE" -> {
                title = "스크랩 공고 변경 안내";
                body = "테스트기관 · 지원 마감일 변경";
                category = "공고 변경";
            }
            case "FAVORITE_NEW" -> {
                title = "테스트기관 신규 채용공고";
                body = "관심 기관 신규 공고 알림 테스트입니다.";
                category = "관심 기관 신규 공고";
            }
            case "IMPORTANT_NOTICE" -> {
                title = "JOB HUB 중요 공지 테스트";
                body = "관리자 중요 공지 알림이 정상적으로 전달되었습니다.";
                category = "중요 공지";
                link = "/community";
            }
            case "DUPLICATE_GUARD" -> {
                title = "알림 중복 방지 테스트";
                body = "이 알림은 한 번만 도착해야 합니다.";
                category = "중복 방지";
            }
            default -> throw new IllegalArgumentException("지원하지 않는 푸시 테스트 유형입니다.");
        }
        String key = "manual-test:" + normalized + ":" + nonce;
        boolean first = deliver(userId, key, title, body, link, category);
        if (!"DUPLICATE_GUARD".equals(normalized)) {
            return new ManualTestResult(true, first, false, title,
                    first ? "시험 알림을 발송했습니다." : "등록된 기기로 시험 알림을 보내지 못했습니다.");
        }
        boolean second = deliver(userId, key, title, body, link, category);
        boolean blocked = first && !second;
        return new ManualTestResult(true, first, blocked, title,
                blocked ? "첫 알림만 발송되고 두 번째 중복 알림은 차단됐습니다."
                        : "중복 방지 결과를 확인하지 못했습니다.");
    }

    private boolean deliver(Long userId, String key, String title, String body, String link, String category) {
        if (receipts.existsByUserIdAndDeliveryKey(userId, key)) return false;
        int success = notifications.sendToUser(userId, title, body, link, category);
        if (success < 1) return false;
        PushDeliveryReceipt receipt = new PushDeliveryReceipt();
        receipt.user = users.getReferenceById(userId);
        receipt.deliveryKey = key;
        receipt.category = category;
        try {
            receipts.saveAndFlush(receipt);
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Push receipt already exists: user={}, key={}", userId, key);
            return false;
        }
    }

    private Set<Integer> deadlineDays(String value) {
        Set<Integer> result = new LinkedHashSet<>();
        if (value != null) for (String item : value.split(",")) {
            try {
                int day = Integer.parseInt(item.trim());
                if (day >= 0 && day <= 30) result.add(day);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.KOREA)
                .replaceAll("^(주식회사|재단법인|사단법인|학교법인|의료법인|\\(주\\)|\\(재\\)|\\(사\\)|㈜)", "")
                .replaceAll("[^가-힣a-z0-9]", "");
    }

    private String summary(String value) {
        if (value == null) return "새로운 공지사항을 확인해 주세요.";
        String clean = value.replace("[[본문 이미지]]", " ").replaceAll("\\s+", " ").trim();
        return clean.length() <= 180 ? clean : clean.substring(0, 180) + "…";
    }
}
