package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByDeviceToken(String deviceToken);
    long countByUserIdAndEnabledTrue(Long userId);
}
