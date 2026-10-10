package kr.co.jobhub.admin.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** 장기 운영 중 변경·알림 이력이 무한히 쌓이지 않도록 하는 전역 보관 정책. */
@Entity
@Table(name = "data_retention_policies")
public class DataRetentionPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public boolean enabled = true;
    @Column(name = "posting_change_days", nullable = false)
    public int postingChangeDays = 180;
    @Column(name = "notification_state_days", nullable = false)
    public int notificationStateDays = 90;
    @Column(name = "last_run_at")
    public Instant lastRunAt;
    @Column(name = "last_posting_changes_deleted", nullable = false)
    public long lastPostingChangesDeleted;
    @Column(name = "last_notification_states_deleted", nullable = false)
    public long lastNotificationStatesDeleted;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
