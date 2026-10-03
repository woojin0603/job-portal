package kr.co.jobhub.model;

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

    @Column(name = "standard_category", nullable = false, length = 40)
    public String standardCategory;

    @Column(name = "original_name", nullable = false, length = 150)
    public String originalName;

    public Integer headcount;

    @Column(length = 150)
    public String workRegion;

    @Column(length = 1000)
    public String requirements;
}
