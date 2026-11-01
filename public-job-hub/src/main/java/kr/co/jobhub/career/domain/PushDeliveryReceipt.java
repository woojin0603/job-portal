package kr.co.jobhub.career.domain;

import jakarta.persistence.*;
import kr.co.jobhub.auth.domain.AppUser;
import java.time.Instant;

/** 성공한 자동 푸시를 기록해 서버 재시작 뒤에도 같은 알림이 중복 발송되지 않게 한다. */
@Entity
@Table(name = "push_delivery_receipts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "delivery_key"}),
        indexes = @Index(name = "idx_push_receipt_delivered", columnList = "delivered_at"))
public class PushDeliveryReceipt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public AppUser user;
    @Column(name = "delivery_key", nullable = false, length = 220)
    public String deliveryKey;
    @Column(nullable = false, length = 60)
    public String category;
    @Column(name = "delivered_at", nullable = false)
    public Instant deliveredAt = Instant.now();
}
