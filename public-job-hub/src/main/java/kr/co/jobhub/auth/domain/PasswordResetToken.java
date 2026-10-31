package kr.co.jobhub.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** 원문 토큰 대신 SHA-256 해시만 저장하는 일회용 비밀번호 재설정 요청이다. */
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_password_reset_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_password_reset_user", columnList = "user_id,created_at")
})
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public AppUser user;

    @Column(name = "token_hash", nullable = false, length = 64)
    public String tokenHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "used_at")
    public Instant usedAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
