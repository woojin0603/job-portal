package kr.co.jobhub.posting.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** 자동 탐색이 어려운 기관의 공식 로고 주소를 운영자가 관리한다. */
@Entity
@Table(name = "institution_logos", uniqueConstraints = @UniqueConstraint(columnNames = "organization"))
public class InstitutionLogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 200)
    public String organization;

    @Column(name = "logo_url", nullable = false, length = 1500)
    public String logoUrl;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
