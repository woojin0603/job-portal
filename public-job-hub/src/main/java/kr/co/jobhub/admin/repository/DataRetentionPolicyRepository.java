package kr.co.jobhub.admin.repository;

import kr.co.jobhub.admin.domain.DataRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataRetentionPolicyRepository extends JpaRepository<DataRetentionPolicy, Long> {
}
