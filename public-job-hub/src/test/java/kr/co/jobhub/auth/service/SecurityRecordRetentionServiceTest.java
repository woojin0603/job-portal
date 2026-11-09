package kr.co.jobhub.auth.service;

import kr.co.jobhub.admin.repository.AdminAuditLogRepository;
import kr.co.jobhub.auth.repository.LoginHistoryRepository;
import kr.co.jobhub.career.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityRecordRetentionServiceTest {
    @Test
    void deletesSecurityRecordsAtDocumentedCutoffs() {
        LoginHistoryRepository logins = mock(LoginHistoryRepository.class);
        AdminAuditLogRepository audits = mock(AdminAuditLogRepository.class);
        PushSubscriptionRepository pushes = mock(PushSubscriptionRepository.class);
        Instant before = Instant.now();

        new SecurityRecordRetentionService(logins, audits, pushes).cleanup();

        Instant after = Instant.now();
        verify(logins).deleteByCreatedAtBefore(argThat(value -> between(value, before.minusSeconds(180L * 86400), after.minusSeconds(180L * 86400))));
        verify(audits).deleteByCreatedAtBefore(argThat(value -> between(value, before.minusSeconds(365L * 86400), after.minusSeconds(365L * 86400))));
        verify(pushes).deleteByEnabledFalseAndUpdatedAtBefore(argThat(value -> between(value, before.minusSeconds(30L * 86400), after.minusSeconds(30L * 86400))));
    }

    private boolean between(Instant value, Instant lower, Instant upper) {
        return !value.isBefore(lower) && !value.isAfter(upper);
    }
}
