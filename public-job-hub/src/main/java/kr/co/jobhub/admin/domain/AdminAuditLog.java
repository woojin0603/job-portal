package kr.co.jobhub.admin.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "admin_audit_logs", indexes = @Index(name = "idx_audit_created", columnList = "created_at"))
public class AdminAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 200) public String administrator;
    @Column(nullable = false, length = 30) public String action;
    @Column(nullable = false, length = 500) public String target;
    @Column(nullable = false) public int status;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}
