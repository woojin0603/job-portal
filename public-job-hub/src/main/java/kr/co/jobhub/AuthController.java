package kr.co.jobhub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.model.AppUser;
import kr.co.jobhub.repo.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * React 클라이언트가 사용하는 회원가입·세션 조회·CSRF 토큰 API다.
 * 실제 비밀번호 검증과 세션 생성은 SecurityConfig의 Spring Security 필터가 처리한다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    /** 회원가입 입력값. 서버에서 이메일 형식과 비밀번호 길이를 검증한다. */
    public record RegisterRequest(@Email @NotBlank String email,
                                  @NotBlank @Size(min = 8, max = 100) String password,
                                  @NotBlank @Size(max = 80) String displayName) {
    }

    /** 비밀번호 해시를 제외하고 화면에 공개할 회원 정보. */
    public record Profile(Long id, String email, String displayName) {
    }

    /** 로그인 여부를 동일한 JSON 구조로 반환하기 위한 응답. */
    public record Session(Profile user) {
    }

    /** 변경 요청의 헤더에 넣을 CSRF 토큰 정보. */
    public record Token(String headerName, String token) {
    }

    private final AppUserRepository users;
    private final PasswordEncoder passwords;

    /** 회원 저장소와 비밀번호 해시 인코더를 주입받는다. */
    public AuthController(AppUserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    /** 브라우저가 로그인·회원가입·스크랩 요청 전에 사용할 CSRF 토큰을 전달한다. */
    @GetMapping("/csrf")
    public Token csrf(CsrfToken token) {
        return new Token(token.getHeaderName(), token.getToken());
    }

    /** 현재 세션이 유효하면 회원 프로필을, 아니면 user가 null인 응답을 반환한다. */
    @GetMapping("/me")
    public Session me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return new Session(null);
        }
        return new Session(users.findByEmail(auth.getName()).map(this::profile).orElse(null));
    }

    /** 신규 계정을 만들고 비밀번호는 BCrypt 해시로 변환해 저장한다. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Profile register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        AppUser user = new AppUser();
        user.email = email;
        user.passwordHash = passwords.encode(request.password());
        user.displayName = request.displayName().trim();
        try {
            return profile(users.save(user));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
    }

    /** DB 엔티티에서 클라이언트에 필요한 안전한 필드만 골라 반환한다. */
    private Profile profile(AppUser user) {
        return new Profile(user.id, user.email, user.displayName);
    }
}
