package kr.co.jobhub.admin.service;

import kr.co.jobhub.admin.domain.DataRetentionPolicy;
import kr.co.jobhub.admin.repository.DataRetentionPolicyRepository;
import kr.co.jobhub.career.repository.UserNotificationStateRepository;
import kr.co.jobhub.posting.repository.PostingChangeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

/** 보관 정책을 조회하고 매일 오래된 운영 데이터를 정리한다. */
@Service
public class DataRetentionService {
    public record CleanupResult(long postingChanges, long notificationStates, Instant executedAt) {}

    private final DataRetentionPolicyRepository policies;
    private final PostingChangeRepository changes;
    private final UserNotificationStateRepository notificationStates;

    public DataRetentionService(DataRetentionPolicyRepository policies, PostingChangeRepository changes,
                                UserNotificationStateRepository notificationStates) {
        this.policies = policies;
        this.changes = changes;
        this.notificationStates = notificationStates;
    }

    public DataRetentionPolicy policy() {
        return policies.findAll().stream().findFirst().orElseGet(() -> policies.save(new DataRetentionPolicy()));
    }

    public Instant cutoff(LocalDate date) {
        return date.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
    }

    @Transactional
    public CleanupResult cleanup(Instant postingCutoff, Instant notificationCutoff) {
        long postingDeleted = changes.deleteByDetectedAtBefore(postingCutoff);
        long notificationDeleted = notificationStates.deleteByUpdatedAtBefore(notificationCutoff);
        Instant now = Instant.now();
        DataRetentionPolicy policy = policy();
        policy.lastRunAt = now;
        policy.lastPostingChangesDeleted = postingDeleted;
        policy.lastNotificationStatesDeleted = notificationDeleted;
        policy.updatedAt = now;
        policies.save(policy);
        return new CleanupResult(postingDeleted, notificationDeleted, now);
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void scheduledCleanup() {
        DataRetentionPolicy policy = policy();
        if (!policy.enabled) return;
        Instant now = Instant.now();
        cleanup(now.minus(Duration.ofDays(policy.postingChangeDays)),
                now.minus(Duration.ofDays(policy.notificationStateDays)));
    }
}
