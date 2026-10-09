package kr.co.jobhub.career.domain;

import jakarta.persistence.*;
import kr.co.jobhub.auth.domain.AppUser;
import java.time.Instant;

/** 계산형 알림의 회원별 읽음·삭제 상태를 영구 보관한다. */
@Entity
@Table(name = "user_notification_states",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notification_key"}),
        indexes = @Index(name = "idx_notification_state_user", columnList = "user_id"))
public class UserNotificationState {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public AppUser user;
    @Column(name = "notification_key", nullable = false, length = 220)
    public String notificationKey;
    @Column(name = "read_at")
    public Instant readAt;
    @Column(name = "deleted_at")
    public Instant deletedAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
