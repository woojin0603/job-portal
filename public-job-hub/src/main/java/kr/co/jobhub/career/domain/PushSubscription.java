package kr.co.jobhub.career.domain;

import jakarta.persistence.*;
import kr.co.jobhub.auth.domain.AppUser;
import java.time.Instant;

/** 모바일 앱에서 FCM 등 푸시 전송에 사용할 회원별 기기 등록 정보다. */
@Entity
@Table(name = "push_subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = "device_token"),
        indexes = @Index(name = "idx_push_subscription_user", columnList = "user_id,enabled"))
public class PushSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public AppUser user;
    @Column(name = "device_token", nullable = false, length = 500)
    public String deviceToken;
    @Column(nullable = false, length = 20)
    public String platform;
    @Column(nullable = false)
    public boolean enabled = true;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
