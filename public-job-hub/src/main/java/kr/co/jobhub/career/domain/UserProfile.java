package kr.co.jobhub.career.domain;

import jakarta.persistence.*;
import kr.co.jobhub.auth.domain.AppUser;
import java.time.Instant;
import java.time.LocalDate;

/** 회원이 직접 입력한 채용 준비 정보를 보관하며 불필요한 식별번호는 저장하지 않는다. */
@Entity
@Table(name = "user_profiles", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public AppUser user;

    public LocalDate birthDate;

    @Lob
    public String certifications;

    @Lob
    public String activities;

    @Lob
    public String careerHistory;

    @Lob
    public String degree;

    @Lob
    public String grades;

    @Column(nullable = false)
    public Instant updatedAt = Instant.now();
}
