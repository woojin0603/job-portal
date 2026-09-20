package kr.co.jobhub.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 외부 채용사이트에서 수집한 채용공고 도메인이다.
 * 같은 출처의 같은 sourceId는 하나의 공고로 취급해 재수집 시 갱신한다.
 */
@Entity
@Table(name = "job_postings", uniqueConstraints = @UniqueConstraint(columnNames = {"source", "source_id"}), indexes = {
        @Index(name = "idx_posting_open", columnList = "open,deadline"),
        @Index(name = "idx_posting_updated", columnList = "updated_at")})
public class JobPosting {
    /** DB 내부 식별자. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 지연 로딩된 JPA 프록시에서도 ID를 안전하게 읽기 위한 접근자. */
    public Long getId() {
        return id;
    }

    /** JOB-ALIO와 같은 수집 출처 이름. */
    @Column(nullable = false, length = 80)
    public String source;

    /** 출처 안에서 공고를 구별하는 원본 식별자. */
    @Column(name = "source_id", nullable = false, length = 300)
    public String sourceId;

    /** 카드 제목과 검색에 사용하는 공고 제목. */
    @Column(nullable = false, length = 300)
    public String title;

    /** 채용을 진행하는 기관명. */
    @Column(nullable = false, length = 200)
    public String organization;

    /** 근무지 및 고용 형태는 원문에 없을 수 있다. */
    @Column(length = 100)
    public String region;

    @Column(length = 100)
    public String employmentType;

    /** 공고 등록일과 지원 마감일. */
    public LocalDate postedAt;
    public LocalDate deadline;

    /** 미니탭 원문 조회에 사용하는 상세 페이지 주소. */
    @Column(nullable = false, length = 1500)
    public String sourceUrl;

    /** 마감일을 기준으로 목록 노출 여부를 판단한다. */
    @Column(nullable = false)
    public boolean open = true;

    /** 최초 발견 시각과 최근 수집 갱신 시각. */
    @Column(name = "first_seen_at", nullable = false)
    public Instant firstSeenAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
