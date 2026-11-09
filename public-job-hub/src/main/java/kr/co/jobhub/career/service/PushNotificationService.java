package kr.co.jobhub.career.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.MessagingErrorCode;
import kr.co.jobhub.career.domain.PushSubscription;
import kr.co.jobhub.career.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 회원 기기 토큰으로 FCM 데이터 메시지를 보내고 만료 토큰을 비활성화한다. */
@Service
public class PushNotificationService {
    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private final PushSubscriptionRepository subscriptions;
    private final ObjectProvider<FirebaseMessaging> messagingProvider;

    public PushNotificationService(PushSubscriptionRepository subscriptions,
                                   ObjectProvider<FirebaseMessaging> messagingProvider) {
        this.subscriptions = subscriptions;
        this.messagingProvider = messagingProvider;
    }

    public boolean enabled() {
        return messagingProvider.getIfAvailable() != null;
    }

    @Transactional
    public int sendToUser(Long userId, String title, String body, String link, String category) {
        FirebaseMessaging messaging = messagingProvider.getIfAvailable();
        if (messaging == null) return 0;
        List<PushSubscription> targets = subscriptions.findByUserIdAndEnabledTrue(userId);
        if (targets.isEmpty()) return 0;

        Map<String, String> data = new LinkedHashMap<>();
        data.put("title", limited(title, 120));
        data.put("body", limited(body, 300));
        data.put("link", link == null || link.isBlank() ? "/" : limited(link, 500));
        data.put("category", category == null ? "알림" : limited(category, 60));
        MulticastMessage message = MulticastMessage.builder()
                .putAllData(data)
                .addAllTokens(targets.stream().map(value -> value.deviceToken).toList())
                .build();
        try {
            BatchResponse response = messaging.sendEachForMulticast(message);
            for (int index = 0; index < response.getResponses().size(); index++) {
                FirebaseMessagingException failure = response.getResponses().get(index).getException();
                if (failure != null && (failure.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                        || failure.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT)) {
                    subscriptions.delete(targets.get(index));
                }
            }
            return response.getSuccessCount();
        } catch (FirebaseMessagingException exception) {
            log.warn("FCM push delivery failed for user {}", userId, exception);
            return 0;
        }
    }

    private String limited(String value, int maximum) {
        String clean = value == null ? "" : value.trim();
        return clean.length() <= maximum ? clean : clean.substring(0, maximum);
    }
}
