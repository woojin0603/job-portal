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

    /** API, 공식 HTML, 공식 PDF 또는 인원 기반 계산값인지 구분한다. */
    @Column(name = "source_type", nullable = false, length = 30,
            columnDefinition = "varchar(30) default 'API'")
    public String sourceType = "API";

    @Column(name = "source_url", length = 1500)
    public String sourceUrl;

    @Column(name = "evidence_text", length = 2000)
    public String evidenceText;

    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean calculated;
}
