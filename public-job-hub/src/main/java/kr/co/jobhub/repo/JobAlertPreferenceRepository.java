package kr.co.jobhub.repo;

import kr.co.jobhub.model.JobAlertPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JobAlertPreferenceRepository extends JpaRepository<JobAlertPreference, Long> {
    Optional<JobAlertPreference> findByUserId(Long userId);
}
