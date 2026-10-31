package kr.co.jobhub.career.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 브라우저에 공개해도 되는 Firebase 웹 앱 식별값과 VAPID 공개키만 제공한다. */
@RestController
public class PushPublicConfigController {
    public record PushConfig(boolean enabled, String apiKey, String authDomain, String projectId,
                             String storageBucket, String messagingSenderId, String appId,
                             String vapidKey) {}

    private final PushConfig config;

    public PushPublicConfigController(
            @Value("${jobhub.push.enabled:false}") boolean enabled,
            @Value("${jobhub.push.web.api-key:}") String apiKey,
            @Value("${jobhub.push.web.auth-domain:}") String authDomain,
            @Value("${jobhub.push.project-id:}") String projectId,
            @Value("${jobhub.push.web.storage-bucket:}") String storageBucket,
            @Value("${jobhub.push.web.messaging-sender-id:}") String messagingSenderId,
            @Value("${jobhub.push.web.app-id:}") String appId,
            @Value("${jobhub.push.web.vapid-key:}") String vapidKey) {
        boolean complete = enabled && !apiKey.isBlank() && !projectId.isBlank()
                && !messagingSenderId.isBlank() && !appId.isBlank() && !vapidKey.isBlank();
        this.config = new PushConfig(complete, apiKey, authDomain, projectId, storageBucket,
                messagingSenderId, appId, vapidKey);
    }

    @GetMapping("/api/push/config")
    public PushConfig config() {
        return config;
    }
}
