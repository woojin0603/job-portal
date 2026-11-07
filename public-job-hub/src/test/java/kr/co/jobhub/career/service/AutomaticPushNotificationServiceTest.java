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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomaticPushNotificationServiceTest {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Long USER_ID = 1L;

    @Mock PushNotificationService notifications;
    @Mock PushSubscriptionRepository subscriptions;
    @Mock PushDeliveryReceiptRepository receipts;
    @Mock JobAlertPreferenceRepository preferences;
    @Mock ScrapRepository scraps;
    @Mock FavoriteOrganizationRepository favorites;
    @Mock JobPostingRepository postings;
    @Mock PostingChangeRepository changes;
    @Mock ServicePopupRepository popups;
    @Mock AppUserRepository users;

    private AutomaticPushNotificationService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.id = USER_ID;
        user.email = "push@example.com";
        user.displayName = "푸시 사용자";

        lenient().when(notifications.enabled()).thenReturn(true);
        lenient().when(notifications.sendToUser(eq(USER_ID), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1);
        lenient().when(subscriptions.findDistinctEnabledUserIds()).thenReturn(List.of(USER_ID));
        lenient().when(preferences.findByUserIdIn(anyCollection())).thenReturn(List.of());
        lenient().when(scraps.findWithUserAndPostingByUserIdIn(anyList())).thenReturn(List.of());
        lenient().when(favorites.findWithUserByUserIdIn(anyCollection())).thenReturn(List.of());
        lenient().when(postings.findByFirstSeenAtAfterOrderByFirstSeenAtDesc(any())).thenReturn(List.of());
        lenient().when(changes.findTop200ByPosting_IdInAndDetectedAtAfterOrderByDetectedAtDesc(anyCollection(), any()))
                .thenReturn(List.of());
        lenient().when(popups.findByActiveTrueAndUpdatedAtAfterOrderByUpdatedAtDesc(any())).thenReturn(List.of());
        lenient().when(receipts.existsByUserIdAndDeliveryKey(eq(USER_ID), anyString())).thenReturn(false);
        lenient().when(users.getReferenceById(USER_ID)).thenReturn(user);

        service = new AutomaticPushNotificationService(notifications, subscriptions, receipts, preferences,
                scraps, favorites, postings, changes, popups, users, true);
    }

    @Test
    void sendsDeadlineNotificationForConfiguredScrappedPosting() {
        JobPosting posting = posting(101L, "마감 임박 채용", "테스트공사");
        posting.deadline = LocalDate.now(SEOUL).plusDays(1);
        Scrap scrap = scrap(posting);
        JobAlertPreference preference = preference();
        preference.deadlineDays = "7,3,1";

        when(preferences.findByUserIdIn(anyCollection())).thenReturn(List.of(preference));
        when(scraps.findWithUserAndPostingByUserIdIn(anyList())).thenReturn(List.of(scrap));

        service.deliverAutomaticNotifications();

        verify(notifications).sendToUser(USER_ID, "스크랩 공고 마감 1일 전",
                "테스트공사 · 마감 임박 채용", "/postings/101", "스크랩 마감");
        verify(receipts).saveAndFlush(argThat(value ->
                value.deliveryKey.equals("deadline:101:" + posting.deadline + ":1")));
    }

    @Test
    void sendsOnlyChangesForTheUsersScrappedPosting() {
        JobPosting scrapped = posting(201L, "스크랩 공고", "관심공단");
        JobPosting other = posting(202L, "다른 공고", "다른기관");
        PostingChange selected = change(301L, scrapped, "지원 마감일");
        PostingChange ignored = change(302L, other, "근무 지역");
        JobAlertPreference preference = preference();
        preference.deadlineAlerts = false;
        preference.postingChangeAlerts = true;

        when(preferences.findByUserIdIn(anyCollection())).thenReturn(List.of(preference));
        when(scraps.findWithUserAndPostingByUserIdIn(anyList())).thenReturn(List.of(scrap(scrapped)));
        when(changes.findTop200ByPosting_IdInAndDetectedAtAfterOrderByDetectedAtDesc(anyCollection(), any()))
                .thenReturn(List.of(selected, ignored));

        service.deliverAutomaticNotifications();

        verify(notifications).sendToUser(USER_ID, "스크랩 공고 변경 안내",
                "관심공단 · 지원 마감일 변경", "/postings/201", "공고 변경");
        verify(notifications, never()).sendToUser(eq(USER_ID), anyString(), contains("다른기관"), anyString(), anyString());
    }

    @Test
    void sendsNewPostingNotificationForFavoriteOrganization() {
        JobPosting posting = posting(401L, "신규 직원 채용", "한국테스트공사");
        posting.firstSeenAt = Instant.now().minusSeconds(60);
        posting.deadline = LocalDate.now(SEOUL).plusDays(10);
        FavoriteOrganization favorite = new FavoriteOrganization();
        favorite.id = 501L;
        favorite.user = user;
        favorite.organizationName = "한국테스트공사";
        favorite.normalizedName = "한국테스트공사";
        favorite.createdAt = Instant.now().minusSeconds(3600);
        JobAlertPreference preference = preference();
        preference.deadlineAlerts = false;
        preference.postingChangeAlerts = false;
        preference.newPostingAlerts = true;

        when(preferences.findByUserIdIn(anyCollection())).thenReturn(List.of(preference));
        when(favorites.findWithUserByUserIdIn(anyCollection())).thenReturn(List.of(favorite));
        when(postings.findByFirstSeenAtAfterOrderByFirstSeenAtDesc(any())).thenReturn(List.of(posting));

        service.deliverAutomaticNotifications();

        verify(notifications).sendToUser(USER_ID, "한국테스트공사 신규 채용공고",
                "신규 직원 채용", "/postings/401", "관심 기관 신규 공고");
    }

    @Test
    void sendsActiveAdministratorNotice() {
        ServicePopup popup = new ServicePopup();
        popup.id = 601L;
        popup.title = "중요 서비스 공지";
        popup.content = "서비스 점검 일정을 안내합니다.";
        popup.active = true;
        popup.updatedAt = Instant.now();
        when(popups.findByActiveTrueAndUpdatedAtAfterOrderByUpdatedAtDesc(any())).thenReturn(List.of(popup));

        service.deliverAutomaticNotifications();

        verify(notifications).sendToUser(USER_ID, "중요 서비스 공지",
                "서비스 점검 일정을 안내합니다.", "/", "중요 공지");
    }

    @Test
    void doesNotSendTheSameNotificationTwice() {
        ServicePopup popup = new ServicePopup();
        popup.id = 701L;
        popup.title = "중복 방지 공지";
        popup.content = "한 번만 발송됩니다.";
        popup.active = true;
        popup.updatedAt = Instant.now();
        AtomicBoolean delivered = new AtomicBoolean(false);
        when(popups.findByActiveTrueAndUpdatedAtAfterOrderByUpdatedAtDesc(any())).thenReturn(List.of(popup));
        when(receipts.existsByUserIdAndDeliveryKey(eq(USER_ID), anyString())).thenAnswer(ignored -> delivered.get());
        when(receipts.saveAndFlush(any())).thenAnswer(invocation -> {
            delivered.set(true);
            return invocation.getArgument(0);
        });

        service.deliverAutomaticNotifications();
        service.deliverAutomaticNotifications();

        verify(notifications, times(1)).sendToUser(USER_ID, "중복 방지 공지",
                "한 번만 발송됩니다.", "/", "중요 공지");
    }

    private JobAlertPreference preference() {
        JobAlertPreference value = new JobAlertPreference();
        value.user = user;
        value.enabled = true;
        return value;
    }

    private Scrap scrap(JobPosting posting) {
        Scrap value = new Scrap();
        value.user = user;
        value.posting = posting;
        return value;
    }

    private JobPosting posting(Long id, String title, String organization) {
        JobPosting value = new JobPosting();
        value.id = id;
        value.source = "TEST";
        value.sourceId = "test-" + id;
        value.title = title;
        value.organization = organization;
        value.sourceUrl = "https://example.com/postings/" + id;
        return value;
    }

    private PostingChange change(Long id, JobPosting posting, String label) {
        PostingChange value = new PostingChange();
        value.id = id;
        value.posting = posting;
        value.fieldLabel = label;
        value.detectedAt = Instant.now();
        return value;
    }
}
