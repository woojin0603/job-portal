package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.UserNotificationState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserNotificationStateRepository extends JpaRepository<UserNotificationState, Long> {
    List<UserNotificationState> findByUserId(Long userId);
    Optional<UserNotificationState> findByUserIdAndNotificationKey(Long userId, String notificationKey);
}
