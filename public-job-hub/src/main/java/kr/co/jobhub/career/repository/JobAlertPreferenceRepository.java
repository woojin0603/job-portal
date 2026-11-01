package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.JobAlertPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface JobAlertPreferenceRepository extends JpaRepository<JobAlertPreference, Long> {
    Optional<JobAlertPreference> findByUserId(Long userId);
    List<JobAlertPreference> findByUserIdIn(Collection<Long> userIds);
}
