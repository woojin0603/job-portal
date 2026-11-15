package kr.co.jobhub.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.jobhub.auth.repository.AppUserRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/** 비밀번호 변경 뒤의 세션 무효화와 관리자 2단계 인증을 검사한다. */
public class AccountSecurityFilter extends OncePerRequestFilter {
    private static final String CHECKED_AT = "ACCOUNT_SECURITY_CHECKED_AT";
    private static final Duration MEMBER_CHECK_INTERVAL = Duration.ofSeconds(20);
    private final AppUserRepository users;
    public AccountSecurityFilter(AppUserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getUserPrincipal() == null) { chain.doFilter(request, response); return; }
        HttpSession session = request.getSession();
        boolean adminRequest = request.getRequestURI().startsWith("/api/admin/");
        Instant checkedAt = session.getAttribute(CHECKED_AT) instanceof Instant value ? value : null;
        boolean refresh = adminRequest || checkedAt == null
                || checkedAt.plus(MEMBER_CHECK_INTERVAL).isBefore(Instant.now());
        if (refresh) {
            var user = users.findByEmail(request.getUserPrincipal().getName()).orElse(null);
            if (user == null) {
                session.invalidate(); response.sendError(401); return;
            } else {
                long userVersion = user.sessionVersion == null ? 0L : user.sessionVersion;
                Object sessionVersion = session.getAttribute("SESSION_VERSION");
                if (sessionVersion == null) session.setAttribute("SESSION_VERSION", userVersion);
                else if (((Number) sessionVersion).longValue() != userVersion) {
                    session.invalidate(); response.sendError(401); return;
                }
                session.setAttribute(CHECKED_AT, Instant.now());
                // 세션에 과거 ROLE_ADMIN이 남아 있어도 DB에서 권한이 회수됐다면 즉시 차단한다.
                if (adminRequest && !user.admin) {
                    response.sendError(403); return;
                }
                if (adminRequest && (!Boolean.TRUE.equals(user.twoFactorEnabled)
                        || !Boolean.TRUE.equals(session.getAttribute("ADMIN_2FA_VERIFIED")))) {
                    response.setStatus(428);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"관리자 2단계 인증이 필요합니다.\",\"requiresTwoFactor\":true}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
