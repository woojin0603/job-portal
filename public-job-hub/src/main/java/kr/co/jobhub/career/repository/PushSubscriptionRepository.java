package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByDeviceToken(String deviceToken);
    Optional<PushSubscription> findByDeviceTokenAndUserId(String deviceToken, Long userId);
    List<PushSubscription> findByUserIdAndEnabledTrue(Long userId);
    long countByUserIdAndEnabledTrue(Long userId);
    @Query("select distinct value.user.id from PushSubscription value where value.enabled = true")
    List<Long> findDistinctEnabledUserIds();
}
