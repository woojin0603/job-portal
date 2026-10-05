package kr.co.jobhub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** 공식 채용 API가 공개한 공고별 전형 지원자 수와 경쟁률을 보존한다. */
@Entity
@Table(name = "recruitment_competitions", indexes = {
        @Index(name = "idx_competition_posting", columnList = "posting_id")})
public class RecruitmentCompetition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false)
    public JobPosting posting;

    @Column(name = "stage_name", nullable = false, length = 500)
    public String stageName;

    public Integer applicants;
    public Integer selected;

    @Column(precision = 12, scale = 2)
    public BigDecimal ratio;
}
