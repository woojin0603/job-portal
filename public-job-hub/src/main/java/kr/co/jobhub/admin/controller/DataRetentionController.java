package kr.co.jobhub.admin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kr.co.jobhub.admin.domain.DataRetentionPolicy;
import kr.co.jobhub.admin.repository.DataRetentionPolicyRepository;
import kr.co.jobhub.admin.service.DataRetentionService;
import kr.co.jobhub.career.repository.UserNotificationStateRepository;
import kr.co.jobhub.posting.repository.PostingChangeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;

/** 관리자가 보관 기간을 설정하고 삭제 전 건수 확인 및 수동 정리를 수행한다. */
@RestController
@RequestMapping("/api/admin/data-retention")
public class DataRetentionController {
    public record PolicyRequest(boolean enabled,
                                @Min(30) @Max(3650) int postingChangeDays,
                                @Min(30) @Max(3650) int notificationStateDays) {}
    public record PolicyView(boolean enabled, int postingChangeDays, int notificationStateDays,
                             Instant lastRunAt, long lastPostingChangesDeleted,
                             long lastNotificationStatesDeleted) {}
    public record CleanupRequest(@NotNull LocalDate postingChangesBefore,
                                 @NotNull LocalDate notificationStatesBefore,
                                 Long expectedPostingChanges, Long expectedNotificationStates) {}
    public record Preview(long postingChanges, long notificationStates) {}

    private final DataRetentionService retention;
    private final DataRetentionPolicyRepository policies;
    private final PostingChangeRepository changes;
    private final UserNotificationStateRepository states;

    public DataRetentionController(DataRetentionService retention, DataRetentionPolicyRepository policies,
                                   PostingChangeRepository changes, UserNotificationStateRepository states) {
        this.retention = retention;
        this.policies = policies;
        this.changes = changes;
        this.states = states;
    }

    @GetMapping
    public PolicyView get() { return view(retention.policy()); }

    @PostMapping
    public PolicyView save(@Valid @RequestBody PolicyRequest request) {
        DataRetentionPolicy value = retention.policy();
        value.enabled = request.enabled();
        value.postingChangeDays = request.postingChangeDays();
        value.notificationStateDays = request.notificationStateDays();
        value.updatedAt = Instant.now();
        return view(policies.save(value));
    }

    @PostMapping("/preview")
    public Preview preview(@Valid @RequestBody CleanupRequest request) {
        validateDates(request);
        return new Preview(changes.countByDetectedAtBefore(retention.cutoff(request.postingChangesBefore())),
                states.countByUpdatedAtBefore(retention.cutoff(request.notificationStatesBefore())));
    }

    @PostMapping("/cleanup")
    public DataRetentionService.CleanupResult cleanup(@Valid @RequestBody CleanupRequest request) {
        validateDates(request);
        Preview current = preview(request);
        if (request.expectedPostingChanges() == null || request.expectedNotificationStates() == null
                || current.postingChanges() != request.expectedPostingChanges()
                || current.notificationStates() != request.expectedNotificationStates()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "삭제 예정 건수가 달라졌습니다. 삭제 전 확인을 다시 실행해 주세요.");
        }
        return retention.cleanup(retention.cutoff(request.postingChangesBefore()),
                retention.cutoff(request.notificationStatesBefore()));
    }

    private PolicyView view(DataRetentionPolicy value) {
        return new PolicyView(value.enabled, value.postingChangeDays, value.notificationStateDays,
                value.lastRunAt, value.lastPostingChangesDeleted, value.lastNotificationStatesDeleted);
    }

    private void validateDates(CleanupRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (request.postingChangesBefore().isAfter(today) || request.notificationStatesBefore().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "미래 날짜를 정리 기준일로 사용할 수 없습니다.");
        }
    }
}
