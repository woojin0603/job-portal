package kr.co.jobhub.common.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import kr.co.jobhub.admin.service.AdminAuditService;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;
public class AdminAuditFilter extends OncePerRequestFilter {
    private final AdminAuditService audit;
    public AdminAuditFilter(AdminAuditService audit) { this.audit = audit; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/") || Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, response);
        if (request.getUserPrincipal() != null) audit.record(request.getUserPrincipal().getName(), request.getMethod(),
                request.getRequestURI(), response.getStatus());
    }
}
