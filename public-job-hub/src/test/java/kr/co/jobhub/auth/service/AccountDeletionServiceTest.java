package kr.co.jobhub.auth.service;

import kr.co.jobhub.admin.service.AdminAuditService;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AccountDeletionServiceTest {
    @Test
    void deletesEveryUserOwnedTableBeforeTheAccount() {
        AppUserRepository users = mock(AppUserRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AdminAuditService audit = mock(AdminAuditService.class);
        AppUser user = new AppUser();
        user.id = 7L;
        user.email = "delete@example.com";
        user.passwordHash = "hash";
        when(users.findByEmail(user.email)).thenReturn(Optional.of(user));
        when(passwords.matches("password", "hash")).thenReturn(true);

        new AccountDeletionService(users, passwords, jdbc, audit)
                .delete(user.email, "password", "탈퇴합니다");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(12)).update(sql.capture(), eq(user.id));
        List<String> statements = sql.getAllValues();
        for (String table : List.of("password_reset_tokens", "posting_reports", "inquiries",
                "push_delivery_receipts", "push_subscriptions", "user_notification_states",
                "posting_change_cursors", "favorite_organizations", "job_alert_preferences",
                "success_stories", "scraps", "user_profiles")) {
            assertTrue(statements.contains("DELETE FROM " + table + " WHERE user_id = ?"), table);
        }
        assertTrue(statements.indexOf("DELETE FROM success_stories WHERE user_id = ?")
                < statements.indexOf("DELETE FROM scraps WHERE user_id = ?"));
        verify(jdbc).update("DELETE FROM login_histories WHERE email = ?", user.email);
        verify(users).delete(user);
        verify(users).flush();
    }
}
