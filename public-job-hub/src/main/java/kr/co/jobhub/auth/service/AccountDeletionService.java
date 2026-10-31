package kr.co.jobhub.auth.service;

import kr.co.jobhub.admin.service.AdminAuditService;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/** 회원이 직접 요청한 계정과 연결 개인정보를 하나의 트랜잭션으로 삭제한다. */
@Service
public class AccountDeletionService {
    private final AppUserRepository users;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;
    private final AdminAuditService audit;

    public AccountDeletionService(AppUserRepository users, PasswordEncoder passwords,
                                  JdbcTemplate jdbc, AdminAuditService audit) {
        this.users = users;
        this.passwords = passwords;
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @Transactional
    public void delete(String email, String password, String confirmation) {
        if (!"탈퇴합니다".equals(confirmation))
            throw new ResponseStatusException(BAD_REQUEST, "확인 문구를 정확히 입력해 주세요.");
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (!passwords.matches(password, user.passwordHash))
            throw new ResponseStatusException(BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");

        Long id = user.id;
        for (String table : new String[]{"password_reset_tokens", "posting_reports", "inquiries", "push_subscriptions",
                "user_notification_states", "posting_change_cursors", "favorite_organizations",
                "job_alert_preferences", "scraps", "user_profiles"}) {
            jdbc.update("DELETE FROM " + table + " WHERE user_id = ?", id);
        }
        jdbc.update("DELETE FROM login_histories WHERE email = ?", user.email);
        users.delete(user);
        users.flush();
        audit.record("deleted-user", "ACCOUNT_DELETE", "self-service", 200);
    }
}
