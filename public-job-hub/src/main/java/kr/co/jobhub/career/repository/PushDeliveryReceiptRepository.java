package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.PushDeliveryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface PushDeliveryReceiptRepository extends JpaRepository<PushDeliveryReceipt, Long> {
    boolean existsByUserIdAndDeliveryKey(Long userId, String deliveryKey);
    long deleteByDeliveredAtBefore(Instant cutoff);
}
