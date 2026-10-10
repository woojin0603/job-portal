package kr.co.jobhub.common.config;
import jakarta.servlet.*;import jakarta.servlet.http.*;import kr.co.jobhub.auth.repository.AppUserRepository;import org.springframework.web.filter.OncePerRequestFilter;import java.io.IOException;
public class AccountSecurityFilter extends OncePerRequestFilter{
 private final AppUserRepository users;public AccountSecurityFilter(AppUserRepository u){users=u;}
 @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse p,FilterChain c)throws ServletException,IOException{
  if(r.getUserPrincipal()!=null){var u=users.findByEmail(r.getUserPrincipal().getName()).orElse(null);if(u!=null){long userVersion=u.sessionVersion==null?0L:u.sessionVersion;Object version=r.getSession().getAttribute("SESSION_VERSION");if(version==null)r.getSession().setAttribute("SESSION_VERSION",userVersion);else if(((Number)version).longValue()!=userVersion){r.getSession().invalidate();p.sendError(401);return;}
   if(Boolean.TRUE.equals(u.twoFactorEnabled)&&r.getRequestURI().startsWith("/api/admin/")&&!Boolean.TRUE.equals(r.getSession().getAttribute("ADMIN_2FA_VERIFIED"))){p.setStatus(428);p.setContentType("application/json;charset=UTF-8");p.getWriter().write("{\"error\":\"관리자 2단계 인증이 필요합니다.\",\"requiresTwoFactor\":true}");return;}}}c.doFilter(r,p);}
}
