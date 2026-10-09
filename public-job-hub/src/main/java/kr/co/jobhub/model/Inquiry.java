package kr.co.jobhub.model;

import jakarta.persistence.*;
import java.time.Instant;

/** 회원이 관리자에게 남기는 비공개 문의와 답변. */
@Entity
@Table(name = "inquiries", indexes = {
        @Index(name = "idx_inquiry_user", columnList = "user_id"),
        @Index(name = "idx_inquiry_status", columnList = "status,created_at")})
public class Inquiry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public AppUser user;
    @Column(nullable = false, length = 30)
    public String category = "OTHER";
    @Column(nullable = false, length = 200)
    public String title;
    @Lob @Column(nullable = false)
    public String content;
    @Column(nullable = false, length = 20)
    public String status = "WAITING";
    @Lob
    public String answer;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
    @Column(name = "answered_at")
    public Instant answeredAt;
}
