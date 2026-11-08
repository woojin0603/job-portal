package kr.co.jobhub.admin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.service.AutomaticPushNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/** 운영자가 자신의 등록 기기로 실제 푸시 형식과 중복 차단을 점검한다. */
@RestController
@RequestMapping("/api/admin/push-tests")
public class AdminPushTestController {
    public record TestRequest(
            @NotBlank
            @Pattern(regexp = "SCRAP_CHANGE|FAVORITE_NEW|IMPORTANT_NOTICE|DUPLICATE_GUARD")
            String scenario) {}

    private final AppUserRepository users;
    private final AutomaticPushNotificationService notifications;

    public AdminPushTestController(AppUserRepository users,
                                   AutomaticPushNotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @PostMapping
    public AutomaticPushNotificationService.ManualTestResult test(@Valid @RequestBody TestRequest request,
                                                                   Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        AppUser user = users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        try {
            return notifications.deliverManualTest(user.id, request.scenario());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
