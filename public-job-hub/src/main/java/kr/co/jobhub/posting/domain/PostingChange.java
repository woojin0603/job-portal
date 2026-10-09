package kr.co.jobhub.posting.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** 재수집 과정에서 확인된 채용공고의 주요 정보 변경 이력. */
@Entity
@Table(name = "posting_changes", indexes = {
        @Index(name = "idx_change_posting", columnList = "posting_id,detected_at"),
        @Index(name = "idx_change_detected", columnList = "detected_at")})
public class PostingChange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false)
    public JobPosting posting;
    @Column(name = "change_type", nullable = false, length = 40)
    public String changeType;
    @Column(name = "field_label", nullable = false, length = 80)
    public String fieldLabel;
    /** IMPORTANT는 지원 판단이나 일정에 직접 영향을 주는 변경, NORMAL은 일반 정보 변경이다. */
    @Column(length = 20)
    public String importance = "NORMAL";
    @Lob @Column(name = "old_value")
    public String oldValue;
    @Lob @Column(name = "new_value")
    public String newValue;
    @Column(name = "source_url", length = 1500)
    public String sourceUrl;
    @Column(name = "detected_at", nullable = false)
    public Instant detectedAt = Instant.now();
}
