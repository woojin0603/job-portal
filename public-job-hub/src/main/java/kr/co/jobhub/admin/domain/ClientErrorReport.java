package kr.co.jobhub.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** 브라우저 렌더링 오류를 개인정보 없이 운영자가 확인하기 위한 제한된 진단 기록이다. */
@Entity
@Table(name = "client_error_reports", indexes = {
        @Index(name = "idx_client_error_occurred", columnList = "occurred_at"),
        @Index(name = "idx_client_error_resolved", columnList = "resolved,occurred_at")
})
public class ClientErrorReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 30)
    public String scope;
    @Column(nullable = false, length = 500)
    public String message;
    @Column(length = 500)
    public String path;
    @Column(length = 500)
    public String userAgent;
    @Column(length = 200)
    public String account;
    @Column(columnDefinition = "TEXT")
    public String stack;
    @Column(name = "component_stack", columnDefinition = "TEXT")
    public String componentStack;
    @Column(name = "occurred_at", nullable = false)
    public Instant occurredAt = Instant.now();
    @Column(nullable = false)
    public boolean resolved;
    @Column(name = "resolved_at")
    public Instant resolvedAt;
}
