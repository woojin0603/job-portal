package kr.co.jobhub.career.domain;

import jakarta.persistence.*;
import kr.co.jobhub.auth.domain.AppUser;
import java.time.Instant;

/** 회원이 신규 채용공고를 받아볼 관심 기관. */
@Entity
@Table(name = "favorite_organizations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "normalized_name"}),
        indexes = @Index(name = "idx_favorite_org_user", columnList = "user_id"))
public class FavoriteOrganization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public AppUser user;
    @Column(name = "organization_name", nullable = false, length = 200)
    public String organizationName;
    @Column(name = "normalized_name", nullable = false, length = 200)
    public String normalizedName;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
    @Column(name = "last_viewed_at")
    public Instant lastViewedAt;
}
