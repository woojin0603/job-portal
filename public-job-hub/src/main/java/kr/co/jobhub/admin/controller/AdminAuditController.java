package kr.co.jobhub.admin.controller;
import kr.co.jobhub.admin.repository.AdminAuditLogRepository;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
@RestController @RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {
    public record AuditItem(Long id, String administrator, String action, String target, int status, Instant createdAt) {}
    private final AdminAuditLogRepository logs;
    public AdminAuditController(AdminAuditLogRepository logs) { this.logs = logs; }
    @GetMapping public List<AuditItem> list() { return logs.findTop100ByOrderByCreatedAtDesc().stream()
            .map(v -> new AuditItem(v.id, v.administrator, v.action, v.target, v.status, v.createdAt)).toList(); }
}
