package kr.co.jobhub.auth.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.auth.repository.LoginHistoryRepository;
import kr.co.jobhub.auth.service.TotpService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

/** 로그인 기록, 다른 세션 종료 및 관리자 2단계 인증 API를 제공한다. */
@RestController
@RequestMapping("/api/auth/security")
public class AccountSecurityController {
    public record History(boolean success, String ipAddress, String userAgent, Instant createdAt) {}
    public record View(boolean twoFactorEnabled, boolean twoFactorVerified, List<History> loginHistory) {}
    public record CodeRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) {}
    public record DisableRequest(@NotBlank String password, @NotBlank String code) {}
    public record Setup(String secret, String otpauthUri) {}

    private static final String VERIFIED_ATTRIBUTE = "ADMIN_2FA_VERIFIED";

    private final AppUserRepository users;
    private final LoginHistoryRepository histories;
    private final TotpService totp;
    private final PasswordEncoder passwords;

    public AccountSecurityController(AppUserRepository users,
                                     LoginHistoryRepository histories,
                                     TotpService totp,
                                     PasswordEncoder passwords) {
        this.users = users;
        this.histories = histories;
        this.totp = totp;
        this.passwords = passwords;
    }

    @GetMapping
    public View view(Principal principal, HttpSession session) {
        AppUser user = user(principal);
        List<History> loginHistory = histories.findTop20ByEmailOrderByCreatedAtDesc(user.email).stream()
                .map(value -> new History(value.success, value.ipAddress, value.userAgent, value.createdAt))
                .toList();
        return new View(Boolean.TRUE.equals(user.twoFactorEnabled),
                Boolean.TRUE.equals(session.getAttribute(VERIFIED_ATTRIBUTE)), loginHistory);
    }

    @PostMapping("/logout-others")
    public void logoutOthers(Principal principal, HttpSession session) {
        AppUser user = user(principal);
        user.sessionVersion = (user.sessionVersion == null ? 0L : user.sessionVersion) + 1;
        users.save(user);
        session.setAttribute("SESSION_VERSION", user.sessionVersion);
    }

    @PostMapping("/2fa/setup")
    public Setup setup(Principal principal) {
        AppUser user = admin(principal);
        user.twoFactorSecret = totp.secret();
        users.save(user);
        String label = URLEncoder.encode("JOB HUB KOREA:" + user.email, StandardCharsets.UTF_8);
        return new Setup(user.twoFactorSecret,
                "otpauth://totp/" + label + "?secret=" + user.twoFactorSecret + "&issuer=JOB%20HUB%20KOREA");
    }

    @PostMapping("/2fa/enable")
    public void enable(@Valid @RequestBody CodeRequest request, Principal principal, HttpSession session) {
        AppUser user = admin(principal);
        requireValidCode(user, request.code());
        user.twoFactorEnabled = true;
        users.save(user);
        session.setAttribute(VERIFIED_ATTRIBUTE, true);
    }

    @PostMapping("/2fa/verify")
    public void verify(@Valid @RequestBody CodeRequest request, Principal principal, HttpSession session) {
        AppUser user = admin(principal);
        if (!Boolean.TRUE.equals(user.twoFactorEnabled)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "2단계 인증이 활성화되어 있지 않습니다.");
        }
        requireValidCode(user, request.code());
        session.setAttribute(VERIFIED_ATTRIBUTE, true);
    }

    @PostMapping("/2fa/disable")
    public void disable(@Valid @RequestBody DisableRequest request,
                        Principal principal,
                        HttpSession session) {
        AppUser user = admin(principal);
        if (!passwords.matches(request.password(), user.passwordHash)
                || !totp.verify(user.twoFactorSecret, request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호 또는 인증 코드를 확인해 주세요.");
        }
        user.twoFactorEnabled = false;
        user.twoFactorSecret = null;
        users.save(user);
        session.removeAttribute(VERIFIED_ATTRIBUTE);
    }

    private void requireValidCode(AppUser user, String code) {
        if (!totp.verify(user.twoFactorSecret, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다.");
        }
    }

    private AppUser admin(Principal principal) {
        if (!(principal instanceof Authentication authentication)
                || authentication.getAuthorities().stream()
                .noneMatch(value -> "ROLE_ADMIN".equals(value.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return user(principal);
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
