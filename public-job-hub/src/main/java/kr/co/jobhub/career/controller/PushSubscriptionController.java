package kr.co.jobhub.career.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.PushSubscription;
import kr.co.jobhub.career.repository.PushSubscriptionRepository;
import kr.co.jobhub.career.service.PushNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;

/** 향후 Android·iOS 앱이 푸시 토큰을 안전하게 등록하고 해제할 API다. */
@RestController
@RequestMapping("/api/tools/push-subscriptions")
public class PushSubscriptionController {
    public record SubscriptionRequest(@NotBlank @Size(max = 500) String deviceToken,
                                      @NotBlank @Pattern(regexp = "ANDROID|IOS|WEB") String platform) {}
    public record SubscriptionStatus(long activeDevices) {}
    public record TestResult(boolean configured, int delivered) {}

    private final PushSubscriptionRepository subscriptions;
    private final AppUserRepository users;
    private final PushNotificationService notifications;

    public PushSubscriptionController(PushSubscriptionRepository subscriptions, AppUserRepository users,
                                      PushNotificationService notifications) {
        this.subscriptions = subscriptions;
        this.users = users;
        this.notifications = notifications;
    }

    @GetMapping
    public SubscriptionStatus status(Principal principal) {
        AppUser user = user(principal);
        return new SubscriptionStatus(subscriptions.countByUserIdAndEnabledTrue(user.id));
    }

    @PostMapping
    public SubscriptionStatus register(@Valid @RequestBody SubscriptionRequest request, Principal principal) {
        AppUser user = user(principal);
        String token = request.deviceToken().trim();
        PushSubscription value = subscriptions.findByDeviceToken(token).orElseGet(PushSubscription::new);
        value.user = user;
        value.deviceToken = token;
        value.platform = request.platform();
        value.enabled = true;
        value.updatedAt = Instant.now();
        subscriptions.save(value);
        return status(principal);
    }

    @PostMapping("/remove")
    public SubscriptionStatus remove(@Valid @RequestBody SubscriptionRequest request, Principal principal) {
        AppUser user = user(principal);
        subscriptions.findByDeviceTokenAndUserId(request.deviceToken().trim(), user.id)
                .ifPresent(subscriptions::delete);
        return status(principal);
    }

    @PostMapping("/test")
    public TestResult test(Principal principal) {
        AppUser user = user(principal);
        int delivered = notifications.sendToUser(user.id, "JOB HUB 알림 테스트",
                "실제 푸시 알림 연결이 완료되었습니다.", "/mypage", "연결 테스트");
        return new TestResult(notifications.enabled(), delivered);
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
