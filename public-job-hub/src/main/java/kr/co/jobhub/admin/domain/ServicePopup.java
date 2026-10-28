package kr.co.jobhub.admin.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** 관리자가 웹 상단 알림과 앱 알림에 함께 노출하는 서비스 공지다. */
@Entity
@Table(name = "service_popups", indexes = @Index(name = "idx_service_popup_active", columnList = "active,created_at"))
public class ServicePopup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 160)
    public String title;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT")
    public String content;
    @Lob @Column(name = "image_data", columnDefinition = "LONGTEXT")
    public String imageData;
    @Column(name = "attachment_name", length = 255)
    public String attachmentName;
    @Column(name = "attachment_type", length = 120)
    public String attachmentType;
    @Lob @Column(name = "attachment_data", columnDefinition = "LONGTEXT")
    public String attachmentData;
    @Column(nullable = false)
    public boolean active = true;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
