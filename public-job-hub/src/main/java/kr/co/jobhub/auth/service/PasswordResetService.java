package kr.co.jobhub.auth.service;

import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.domain.PasswordResetToken;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.auth.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** 비밀번호 재설정 토큰 발급·메일 발송·일회용 검증을 담당한다. */
@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Duration REQUEST_INTERVAL = Duration.ofMinutes(5);

    private final AppUserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String publicUrl;
    private final String fromAddress;

    public PasswordResetService(AppUserRepository users,
                                PasswordResetTokenRepository tokens,
                                PasswordEncoder passwords,
                                JavaMailSender mailSender,
                                @Value("${jobhub.password-reset.mail-enabled:false}") boolean mailEnabled,
                                @Value("${jobhub.password-reset.public-url:http://localhost:8080}") String publicUrl,
                                @Value("${jobhub.password-reset.from:no-reply@localhost}") String fromAddress) {
        this.users = users;
        this.tokens = tokens;
        this.passwords = passwords;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress;
    }

    public boolean mailEnabled() {
        return mailEnabled;
    }

    @Transactional
    public void request(String email) {
        deleteExpiredTokens();
        if (!mailEnabled) return;
        String normalizedEmail = email.trim().toLowerCase();
        users.findByEmail(normalizedEmail).ifPresent(user -> issue(user, normalizedEmail));
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredTokens() {
        tokens.deleteByExpiresAtBefore(Instant.now());
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).{10,100}$")) {
            throw new ResponseStatusException(BAD_REQUEST, "비밀번호는 영문과 숫자를 포함해 10자 이상이어야 합니다.");
        }
        PasswordResetToken token = tokens.findByTokenHashAndUsedAtIsNull(hash(rawToken))
                .filter(value -> value.expiresAt.isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "재설정 링크가 만료되었거나 이미 사용되었습니다."));
        // PasswordResetToken.user is a lazy Hibernate proxy. A repository find in the same
        // persistence context can return that same proxy, so update the security fields directly.
        Long userId = token.user.getId();
        token.usedAt = Instant.now();
        tokens.saveAndFlush(token);
        int updated = users.resetPassword(userId, passwords.encode(newPassword));
        if (updated != 1) {
            throw new ResponseStatusException(BAD_REQUEST, "재설정할 계정을 찾을 수 없습니다.");
        }
        log.info("Password reset completed for user id {}", userId);
    }

    private void issue(AppUser user, String recipientAddress) {
        if (tokens.countByUserIdAndCreatedAtAfter(user.id, Instant.now().minus(REQUEST_INTERVAL)) > 0) return;
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken token = new PasswordResetToken();
        token.user = user;
        token.tokenHash = hash(rawToken);
        token.expiresAt = Instant.now().plus(TOKEN_LIFETIME);
        tokens.save(token);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipientAddress);
            message.setSubject("[JOB HUB KOREA] 비밀번호 재설정");
            message.setText("아래 링크에서 30분 안에 비밀번호를 재설정해 주세요.\n\n"
                    // URL fragments are not sent to the web server or retained in access logs.
                    + publicUrl + "/#resetToken=" + rawToken
                    + "\n\n본인이 요청하지 않았다면 이 메일을 무시하세요.");
            mailSender.send(message);
        } catch (MailException | IllegalArgumentException exception) {
            tokens.delete(token);
            log.warn("Password reset email delivery failed. Check SMTP configuration.", exception);
        }
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) throw new ResponseStatusException(BAD_REQUEST, "재설정 토큰이 필요합니다.");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
