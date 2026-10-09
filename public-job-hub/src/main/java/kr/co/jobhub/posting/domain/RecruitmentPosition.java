package kr.co.jobhub.posting.domain;

import jakarta.persistence.*;

/** 한 공고 안에서 모집하는 개별 직렬과 원문 근거를 보존한다. */
@Entity
@Table(name = "recruitment_positions", indexes = {
        @Index(name = "idx_position_posting", columnList = "posting_id"),
        @Index(name = "idx_position_category", columnList = "standard_category")})
public class RecruitmentPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false)
    public JobPosting posting;

    /** 공식 API는 여러 NCS 대분류를 쉼표로 묶어 제공할 수 있다. */
    @Column(name = "standard_category", nullable = false, length = 500)
    public String standardCategory;

    @Column(name = "original_name", nullable = false, length = 500)
    public String originalName;

    public Integer headcount;

    @Column(length = 500)
    public String workRegion;

    /** 공고 API의 지원자격 원문은 1,000자를 넘을 수 있으므로 축약하지 않고 대용량 텍스트로 저장한다. */
    @Lob
    @Column
    public String requirements;
}
