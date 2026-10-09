package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.JobAlertPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JobAlertPreferenceRepository extends JpaRepository<JobAlertPreference, Long> {
    Optional<JobAlertPreference> findByUserId(Long userId);
}
