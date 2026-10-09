package kr.co.jobhub.career.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.UserNotificationState;
import kr.co.jobhub.career.repository.UserNotificationStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

/** 상단 알림센터의 개별 읽음 및 숨김 상태를 계정별로 관리한다. */
@RestController
@RequestMapping("/api/tools/notification-states")
public class NotificationStateController {
    public record StateItem(String key, boolean read, boolean deleted) {}
    public record KeysRequest(@NotEmpty @Size(max = 100) List<@Size(max = 220) String> keys) {}

    private final UserNotificationStateRepository states;
    private final AppUserRepository users;

    public NotificationStateController(UserNotificationStateRepository states, AppUserRepository users) {
        this.states = states;
        this.users = users;
    }

    @GetMapping
    public List<StateItem> list(Principal principal) {
        AppUser user = user(principal);
        return states.findByUserId(user.id).stream()
                .map(value -> new StateItem(value.notificationKey, value.readAt != null, value.deletedAt != null))
                .toList();
    }

    @PostMapping("/read")
    @Transactional
    public void read(@Valid @RequestBody KeysRequest request, Principal principal) {
        update(request.keys(), user(principal), false);
    }

    @PostMapping("/delete")
    @Transactional
    public void delete(@Valid @RequestBody KeysRequest request, Principal principal) {
        update(request.keys(), user(principal), true);
    }

    private void update(List<String> keys, AppUser user, boolean deleted) {
        Instant now = Instant.now();
        keys.stream().filter(key -> key != null && !key.isBlank()).distinct().forEach(key -> {
            UserNotificationState state = states.findByUserIdAndNotificationKey(user.id, key)
                    .orElseGet(UserNotificationState::new);
            state.user = user;
            state.notificationKey = key;
            state.readAt = now;
            if (deleted) state.deletedAt = now;
            state.updatedAt = now;
            states.save(state);
        });
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
