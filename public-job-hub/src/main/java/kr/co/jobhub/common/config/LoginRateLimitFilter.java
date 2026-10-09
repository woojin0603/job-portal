package kr.co.jobhub.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/** 한 IP·계정 조합의 반복 로그인 실패를 메모리에서 제한해 무차별 대입을 완화한다. */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private record Attempt(int failures, Instant windowStarted) {}
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;

    public LoginRateLimitFilter(int maxFailures, Duration window) {
        this.maxFailures = maxFailures;
        this.window = window;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !"/api/auth/login".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String safeUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (safeUsername.length() > 255) safeUsername = safeUsername.substring(0, 255);
        String key = request.getRemoteAddr() + "|" + safeUsername;
        Instant now = Instant.now();
        Attempt current = attempts.get(key);
        if (current != null && current.windowStarted().plus(window).isAfter(now) && current.failures() >= maxFailures) {
            response.setStatus(429);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
            response.getWriter().write("{\"error\":\"로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.\"}");
            return;
        }
        chain.doFilter(request, response);
        if (response.getStatus() >= 200 && response.getStatus() < 300) attempts.remove(key);
        else if (response.getStatus() == 401) attempts.compute(key, (ignored, previous) -> previous == null
                || previous.windowStarted().plus(window).isBefore(now)
                ? new Attempt(1, now) : new Attempt(previous.failures() + 1, previous.windowStarted()));
        if (attempts.size() > 10_000) attempts.entrySet().removeIf(entry -> entry.getValue().windowStarted().plus(window).isBefore(now));
    }
}
