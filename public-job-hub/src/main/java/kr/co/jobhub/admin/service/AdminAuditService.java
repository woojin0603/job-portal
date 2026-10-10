package kr.co.jobhub.admin.service;
import kr.co.jobhub.admin.domain.AdminAuditLog;
import kr.co.jobhub.admin.repository.AdminAuditLogRepository;
import org.springframework.stereotype.Service;
@Service
public class AdminAuditService {
    private final AdminAuditLogRepository logs;
    public AdminAuditService(AdminAuditLogRepository logs) { this.logs = logs; }
    public void record(String administrator, String action, String target, int status) {
        AdminAuditLog value = new AdminAuditLog(); value.administrator = administrator;
        value.action = action; value.target = target; value.status = status; logs.save(value);
    }
}
