package kr.co.jobhub.success.domain;

import jakarta.persistence.*;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.career.domain.Scrap;
import kr.co.jobhub.posting.domain.JobPosting;
import java.time.Instant;

/** 회원이 서류 합격 경험을 익명으로 나누는 게시물. */
@Entity
@Table(name = "success_stories", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "scrap_id"}),
        indexes = @Index(name = "idx_success_story_created", columnList = "created_at"))
public class SuccessStory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") public AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "scrap_id") public Scrap scrap;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "posting_id") public JobPosting posting;
    @Column(nullable = false, length = 120) public String title;
    @Column(nullable = false, length = 5000) public String content;
    @Lob @Column(name = "shared_profile") public String sharedProfile;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) public Instant updatedAt = Instant.now();
}
