package kr.co.jobhub.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 서비스 회원을 나타내는 도메인이다.
 * 이메일은 로그인 ID로 사용하며 비밀번호 원문 대신 BCrypt 해시만 저장한다.
 */
@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class AppUser {
    /** 내부 참조용 회원 식별자. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 중복을 허용하지 않는 로그인 이메일. */
    @Column(nullable = false, length = 255)
    public String email;

    /** 인증 시 비교할 암호화된 비밀번호. */
    @Column(name = "password_hash", length = 255)
    public String passwordHash;

    /** 화면에 표시하는 회원 이름. */
    @Column(nullable = false, length = 80)
    public String displayName;

    /** 기본 관리자 비밀번호를 계속 사용하는 계정은 변경 화면을 우선 노출한다. */
    @Column(name = "must_change_password", nullable = false, columnDefinition = "boolean default false")
    public boolean mustChangePassword;

    /** 회원 레코드가 처음 생성된 시각. */
    @Column(nullable = false)
    public Instant createdAt = Instant.now();
}
