package kr.co.jobhub.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityAttemptLimiterTest {
    @Test
    void blocksAtLimitAndCanBeClearedAfterSuccessfulVerification() {
        SecurityAttemptLimiter limiter = new SecurityAttemptLimiter();

        limiter.require("two-factor", "USER@example.com", 2, Duration.ofMinutes(1));
        limiter.require("two-factor", "user@example.com", 2, Duration.ofMinutes(1));
        assertThrows(ResponseStatusException.class,
                () -> limiter.require("two-factor", "user@example.com", 2, Duration.ofMinutes(1)));

        limiter.clear("two-factor", "USER@example.com");
        assertDoesNotThrow(() -> limiter.require(
                "two-factor", "user@example.com", 2, Duration.ofMinutes(1)));
    }
}
