package kr.co.jobhub.admin.controller;

import kr.co.jobhub.admin.repository.AdminAuditLogRepository;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.domain.LoginHistory;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.auth.repository.LoginHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 비밀번호·IP 원문을 노출하지 않고 관리자 보안 지표만 제공한다. */
@RestController
@RequestMapping("/api/admin/security/overview")
public class AdminSecurityOverviewController {
    public record DailyFailures(LocalDate date, long failures) {}
    public record SecurityCheck(String key, String label, boolean enabled, String detail) {}
    public record SecurityOverview(long adminAccounts, long twoFactorEnabled, long passwordChangeRequired,
                                   long failedLogins24h, long successfulLogins24h, long distinctFailureSources24h,
                                   long failedAdminRequests24h, List<DailyFailures> loginFailures,
                                   List<SecurityCheck> checks, Instant checkedAt) {}

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final AppUserRepository users;
    private final LoginHistoryRepository logins;
    private final AdminAuditLogRepository audits;
    private final Set<String> administratorEmails;
    private final int loginMaxFailures;
    private final long sessionTimeoutMinutes;

    public AdminSecurityOverviewController(
            AppUserRepository users, LoginHistoryRepository logins, AdminAuditLogRepository audits,
            @Value("${jobhub.admin.emails:}") String adminEmails,
            @Value("${jobhub.admin.username:admin}") String adminUsername,
            @Value("${jobhub.security.login-max-failures:5}") int loginMaxFailures,
            @Value("${server.servlet.session.timeout:30m}") java.time.Duration sessionTimeout) {
        this.users = users;
        this.logins = logins;
        this.audits = audits;
        this.administratorEmails = Arrays.stream((adminEmails + "," + adminUsername).split(","))
                .map(String::trim).map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank()).collect(Collectors.toSet());
        this.loginMaxFailures = loginMaxFailures;
        this.sessionTimeoutMinutes = Math.max(1, sessionTimeout.toMinutes());
    }

    @GetMapping
    public SecurityOverview overview() {
        Instant now = Instant.now();
        Instant since24h = now.minusSeconds(24 * 60 * 60);
        List<AppUser> administrators = users.findAll().stream()
                .filter(value -> administratorEmails.contains(value.email.toLowerCase(Locale.ROOT))).toList();
        Instant weekStart = todayStart(now).minusSeconds(6 * 24 * 60 * 60);
        List<LoginHistory> histories = logins.findByCreatedAtAfter(weekStart);
        List<LoginHistory> recent = histories.stream()
                .filter(value -> value.createdAt != null && !value.createdAt.isBefore(since24h)).toList();

        LocalDate today = LocalDate.now(SEOUL);
        Map<LocalDate, Long> failures = new LinkedHashMap<>();
        for (int offset = 6; offset >= 0; offset--) failures.put(today.minusDays(offset), 0L);
        histories.stream().filter(value -> !value.success && value.createdAt != null).forEach(value -> {
            LocalDate date = value.createdAt.atZone(SEOUL).toLocalDate();
            if (failures.containsKey(date)) failures.compute(date, (key, count) -> count + 1);
        });

        long recentFailures = recent.stream().filter(value -> !value.success).count();
        return new SecurityOverview(administrators.size(),
                administrators.stream().filter(value -> Boolean.TRUE.equals(value.twoFactorEnabled)).count(),
                administrators.stream().filter(value -> value.mustChangePassword).count(),
                recentFailures, recent.stream().filter(value -> value.success).count(),
                recent.stream().filter(value -> !value.success)
                        .map(value -> value.ipAddress == null ? "unknown" : value.ipAddress).distinct().count(),
                audits.countByCreatedAtAfterAndStatusGreaterThanEqual(since24h, 400),
                failures.entrySet().stream().map(value -> new DailyFailures(value.getKey(), value.getValue())).toList(),
                List.of(
                        new SecurityCheck("csrf", "CSRF 요청 보호", true, "상태 변경 요청에 보안 토큰 적용"),
                        new SecurityCheck("csp", "브라우저 보안 정책", true, "CSP·Referrer Policy 적용"),
                        new SecurityCheck("rate-limit", "로그인 시도 제한", loginMaxFailures >= 3,
                                loginMaxFailures + "회 실패 기준"),
                        new SecurityCheck("session", "관리자 세션 만료", sessionTimeoutMinutes <= 30,
                                sessionTimeoutMinutes + "분 후 만료"),
                        new SecurityCheck("two-factor", "관리자 2단계 인증",
                                !administrators.isEmpty() && administrators.stream()
                                        .allMatch(value -> Boolean.TRUE.equals(value.twoFactorEnabled)),
                                administrators.stream().filter(value -> Boolean.TRUE.equals(value.twoFactorEnabled)).count()
                                        + "/" + administrators.size() + " 계정 적용")), now);
    }

    private Instant todayStart(Instant now) {
        return now.atZone(SEOUL).toLocalDate().atStartOfDay(SEOUL).toInstant();
    }
}
