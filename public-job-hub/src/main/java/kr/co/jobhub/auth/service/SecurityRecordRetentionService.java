package kr.co.jobhub.auth.service;

import kr.co.jobhub.admin.repository.AdminAuditLogRepository;
import kr.co.jobhub.auth.repository.LoginHistoryRepository;
import kr.co.jobhub.career.repository.PushSubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** 보안·감사·비활성 기기 기록이 명시한 목적보다 오래 남지 않도록 매일 정리한다. */
@Service
public class SecurityRecordRetentionService {
    static final Duration LOGIN_HISTORY_RETENTION = Duration.ofDays(180);
    static final Duration ADMIN_AUDIT_RETENTION = Duration.ofDays(365);
    static final Duration DISABLED_PUSH_TOKEN_RETENTION = Duration.ofDays(30);

    private final LoginHistoryRepository loginHistories;
    private final AdminAuditLogRepository adminAudits;
    private final PushSubscriptionRepository pushSubscriptions;

    public SecurityRecordRetentionService(LoginHistoryRepository loginHistories,
                                          AdminAuditLogRepository adminAudits,
                                          PushSubscriptionRepository pushSubscriptions) {
        this.loginHistories = loginHistories;
        this.adminAudits = adminAudits;
        this.pushSubscriptions = pushSubscriptions;
    }

    @Scheduled(cron = "0 20 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        loginHistories.deleteByCreatedAtBefore(now.minus(LOGIN_HISTORY_RETENTION));
        adminAudits.deleteByCreatedAtBefore(now.minus(ADMIN_AUDIT_RETENTION));
        pushSubscriptions.deleteByEnabledFalseAndUpdatedAtBefore(now.minus(DISABLED_PUSH_TOKEN_RETENTION));
    }
}
