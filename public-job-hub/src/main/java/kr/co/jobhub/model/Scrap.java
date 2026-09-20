package kr.co.jobhub.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 회원과 채용공고 사이의 개인 보관 관계다.
 * 지원 완료 상태는 공고 자체가 아닌 각 회원의 스크랩에 저장된다.
 */
@Entity
@Table(name = "scraps", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "posting_id"}))
public class Scrap {
    /** 스크랩 레코드의 식별자. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 이 공고를 보관한 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public AppUser user;

    /** 보관 대상 공고. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id")
    public JobPosting posting;

    /** 회원이 직접 표시한 지원 완료 여부. */
    @Column(nullable = false)
    public boolean applied;

    /** 스크랩한 시각과 지원 완료로 표시한 시각. */
    @Column(name = "scrapped_at", nullable = false)
    public Instant scrappedAt = Instant.now();

    public Instant appliedAt;
}
