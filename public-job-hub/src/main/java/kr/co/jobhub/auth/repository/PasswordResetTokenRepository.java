package kr.co.jobhub.auth.repository;

import kr.co.jobhub.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);
    long countByUserIdAndCreatedAtAfter(Long userId, Instant after);

    @Transactional
    void deleteByExpiresAtBefore(Instant cutoff);
}
