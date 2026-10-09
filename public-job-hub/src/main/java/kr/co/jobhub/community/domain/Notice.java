package kr.co.jobhub.community.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** 관리자가 사용자에게 안내하는 서비스 공지사항. */
@Entity
@Table(name = "notices", indexes = @Index(name = "idx_notice_created", columnList = "created_at"))
public class Notice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 200)
    public String title;
    @Lob @Column(nullable = false)
    public String content;
    @Column(nullable = false)
    public boolean pinned;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
