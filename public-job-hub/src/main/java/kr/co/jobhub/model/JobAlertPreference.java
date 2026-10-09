package kr.co.jobhub.model;

import jakarta.persistence.*;
import java.time.Instant;

/** 회원별 맞춤 공고 조건과 마지막 확인 시각을 저장한다. */
@Entity
@Table(name = "job_alert_preferences", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class JobAlertPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public AppUser user;

    @Column(length = 500)
    public String keywords;

    @Column(length = 500)
    public String regions;

    @Column(length = 100)
    public String mobilityTypes;

    @Column(nullable = false)
    public boolean enabled = true;

    @Column(name = "new_posting_alerts", nullable = false, columnDefinition = "boolean default true")
    public boolean newPostingAlerts = true;

    @Column(name = "deadline_alerts", nullable = false, columnDefinition = "boolean default true")
    public boolean deadlineAlerts = true;

    @Column(name = "deadline_days", nullable = false, length = 30,
            columnDefinition = "varchar(30) default '7,3,1'")
    public String deadlineDays = "7,3,1";

    public Instant lastViewedAt;
    public Instant updatedAt = Instant.now();
}
