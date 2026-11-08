package kr.co.jobhub;

import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.domain.PasswordResetToken;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.auth.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:passwordresettest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.mail.host=localhost",
        "jobhub.crawl.enabled=false",
        "jobhub.password-reset.mail-enabled=false"
})
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {
    private static final String EMAIL = "password-reset@example.com";
    private static final String OLD_PASSWORD = "OldPassword123";
    private static final String NEW_PASSWORD = "NewPassword456";
    private static final String RAW_TOKEN = "release-test-reset-token";

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder passwords;

    @BeforeEach
    void prepareAccountAndToken() throws Exception {
        tokens.deleteAll();
        users.deleteAll();
        AppUser user = new AppUser();
        user.email = EMAIL;
        user.displayName = "재설정 테스트";
        user.passwordHash = passwords.encode(OLD_PASSWORD);
        user = users.saveAndFlush(user);

        PasswordResetToken token = new PasswordResetToken();
        token.user = user;
        token.tokenHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(RAW_TOKEN.getBytes(StandardCharsets.UTF_8)));
        token.expiresAt = Instant.now().plusSeconds(1800);
        tokens.saveAndFlush(token);
    }

    @Test
    void resetRejectsOldPasswordAndAcceptsNewPassword() throws Exception {
        mvc.perform(post("/api/auth/password-reset/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + RAW_TOKEN + "\",\"newPassword\":\""
                                + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", EMAIL).param("password", OLD_PASSWORD))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", EMAIL).param("password", NEW_PASSWORD))
                .andExpect(status().isOk());
    }
}
