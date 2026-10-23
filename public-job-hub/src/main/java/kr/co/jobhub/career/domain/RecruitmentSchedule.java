package kr.co.jobhub.career.domain;

import jakarta.persistence.*;
import kr.co.jobhub.posting.domain.JobPosting;
import java.time.Instant;
import java.time.LocalDate;

/** 공개 채용공고 원문에서 근거 문장과 함께 자동 추출한 전형 일정. */
@Entity
@Table(name = "recruitment_schedules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"posting_id", "schedule_type", "event_date"}),
        indexes = @Index(name = "idx_recruitment_schedule_date", columnList = "event_date"))
public class RecruitmentSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "posting_id") public JobPosting posting;
    @Column(name = "schedule_type", nullable = false, length = 30) public String scheduleType;
    @Column(nullable = false, length = 80) public String label;
    @Column(name = "event_date", nullable = false) public LocalDate eventDate;
    @Column(nullable = false) public boolean tentative;
    @Column(nullable = false, length = 1000) public String evidence;
    @Column(name = "source_url", nullable = false, length = 1500) public String sourceUrl;
    @Column(name = "detected_at", nullable = false) public Instant detectedAt = Instant.now();
}
