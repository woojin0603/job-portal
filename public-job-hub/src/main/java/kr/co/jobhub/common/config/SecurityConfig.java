package kr.co.jobhub.common.config;

import kr.co.jobhub.auth.repository.AppUserRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import java.time.Duration;

/**
 * 세션 로그인, 비밀번호 검증, CSRF 및 API 접근 권한을 구성한다.
 * 목록 조회는 공개하되 스크랩 변경은 인증된 회원에게만 허용한다.
 */
@Configuration
public class SecurityConfig {
    /** 계정 비밀번호의 저장과 검증에 사용할 BCrypt 인코더. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 이메일로 회원을 읽어 Spring Security가 이해하는 인증 사용자로 변환한다. */
    @Bean
    UserDetailsService userDetailsService(AppUserRepository users,
                                          @org.springframework.beans.factory.annotation.Value("${jobhub.admin.emails:}") String adminEmails,
                                          @org.springframework.beans.factory.annotation.Value("${jobhub.admin.username:admin}") String adminUsername) {
        var admins = java.util.Arrays.stream(adminEmails.split(","))
                .map(String::trim).map(String::toLowerCase).filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.toSet());
        admins.add(adminUsername.trim().toLowerCase());
        return email -> users.findByEmail(email)
                .filter(u -> u.passwordHash != null)
                .map(u -> User.withUsername(u.email).password(u.passwordHash)
                        .roles(admins.contains(u.email.toLowerCase()) ? new String[]{"USER", "ADMIN"} : new String[]{"USER"}).build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(email));
    }

    /** 변경 요청 위조를 막는 토큰을 서버 세션에 보관한다. */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    LoginRateLimitFilter loginRateLimitFilter(
            @org.springframework.beans.factory.annotation.Value("${jobhub.security.login-max-failures:5}") int maxFailures,
            @org.springframework.beans.factory.annotation.Value("${jobhub.security.login-window-minutes:15}") long windowMinutes) {
        return new LoginRateLimitFilter(Math.max(3, maxFailures), Duration.ofMinutes(Math.max(1, windowMinutes)));
    }

    /** 보안 체인에만 필터를 넣고 서블릿 컨테이너의 중복 자동 등록은 막는다. */
    @Bean
    FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilterRegistration(LoginRateLimitFilter filter) {
        FilterRegistrationBean<LoginRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** 로그인·로그아웃을 JSON 응답으로 처리하고 경로별 접근 규칙을 적용한다. */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository tokens,
                                            LoginRateLimitFilter loginRateLimitFilter) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(tokens))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(policy -> policy.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data: https:; connect-src 'self'; frame-src 'self' https:; object-src 'none'; base-uri 'self'; form-action 'self'")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/community/notices").permitAll()
                        .requestMatchers("/api/community/inquiries", "/api/community/inquiries/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/postings/**").authenticated()
                        .requestMatchers("/api/profile", "/api/profile/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tools/filters").permitAll()
                        .requestMatchers("/api/tools/**").authenticated()
                        .requestMatchers("/h2-console/**").denyAll()
                        .anyRequest().permitAll())
                .formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"ok\":true}");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"이메일 또는 비밀번호가 올바르지 않습니다.\"}");
                        }))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"ok\":true}");
                        }))
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, exception) ->
                        response.sendError(401)));
        http.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
