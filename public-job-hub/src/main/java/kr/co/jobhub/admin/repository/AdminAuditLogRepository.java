package kr.co.jobhub.admin.repository;
import kr.co.jobhub.admin.domain.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findTop100ByOrderByCreatedAtDesc();
    long countByCreatedAtAfterAndStatusGreaterThanEqual(Instant createdAt, int status);
}
