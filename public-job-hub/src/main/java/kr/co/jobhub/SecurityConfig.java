package kr.co.jobhub;

import kr.co.jobhub.repo.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

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
    UserDetailsService userDetailsService(AppUserRepository users) {
        return email -> users.findByEmail(email)
                .filter(u -> u.passwordHash != null)
                .map(u -> User.withUsername(u.email).password(u.passwordHash).roles("USER").build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(email));
    }

    /** 변경 요청 위조를 막는 토큰을 서버 세션에 보관한다. */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    /** 로그인·로그아웃을 JSON 응답으로 처리하고 경로별 접근 규칙을 적용한다. */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository tokens) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(tokens))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/postings/**").authenticated()
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
        return http.build();
    }
}
