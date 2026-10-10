package kr.co.jobhub.career.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.PushSubscription;
import kr.co.jobhub.career.repository.PushSubscriptionRepository;
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

    private final PushSubscriptionRepository subscriptions;
    private final AppUserRepository users;

    public PushSubscriptionController(PushSubscriptionRepository subscriptions, AppUserRepository users) {
        this.subscriptions = subscriptions;
        this.users = users;
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
        subscriptions.findByDeviceToken(request.deviceToken().trim())
                .filter(value -> value.user.id.equals(user.id))
                .ifPresent(value -> {
                    value.enabled = false;
                    value.updatedAt = Instant.now();
                    subscriptions.save(value);
                });
        return status(principal);
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
