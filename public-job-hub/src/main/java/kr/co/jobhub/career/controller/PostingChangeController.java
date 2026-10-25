package kr.co.jobhub.career.controller;

import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.*;
import kr.co.jobhub.career.repository.*;
import kr.co.jobhub.posting.domain.PostingChange;
import kr.co.jobhub.posting.repository.PostingChangeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** 회원이 직접 스크랩한 공고의 변경 알림만 제공한다. */
@RestController
@RequestMapping("/api/tools/posting-changes")
public class PostingChangeController {
    public record ChangeItem(Long id, Long postingId, String title, String organization,
                             String fieldLabel, String importance, String oldValue, String newValue,
                             String sourceUrl, Instant detectedAt, boolean fresh) {}
    public record UnreadCount(long count) {}

    private final PostingChangeRepository changes;
    private final PostingChangeCursorRepository cursors;
    private final ScrapRepository scraps;
    private final AppUserRepository users;

    public PostingChangeController(PostingChangeRepository changes, PostingChangeCursorRepository cursors,
                                   ScrapRepository scraps, AppUserRepository users) {
        this.changes = changes;
        this.cursors = cursors;
        this.scraps = scraps;
        this.users = users;
    }

    @GetMapping
    public List<ChangeItem> list(Principal principal) {
        AppUser user = user(principal);
        Instant viewedAt = cursors.findByUserId(user.id).map(value -> value.lastViewedAt).orElse(null);
        Set<Long> postingIds = scraps.findByUserId(user.id).stream()
                .map(value -> value.posting.id).collect(Collectors.toSet());
        if (postingIds.isEmpty()) return List.of();
        return changes.findTop100ByPosting_IdInOrderByDetectedAtDesc(postingIds).stream()
                .map(value -> item(value, viewedAt)).toList();
    }

    @GetMapping("/unread-count")
    public UnreadCount unreadCount(Principal principal) {
        return new UnreadCount(list(principal).stream().filter(ChangeItem::fresh).count());
    }

    @PostMapping("/read")
    public void read(Principal principal) {
        AppUser user = user(principal);
        PostingChangeCursor cursor = cursors.findByUserId(user.id).orElseGet(PostingChangeCursor::new);
        cursor.user = user;
        cursor.lastViewedAt = Instant.now();
        cursors.save(cursor);
    }

    private ChangeItem item(PostingChange value, Instant viewedAt) {
        return new ChangeItem(value.id, value.posting.id, value.posting.title, value.posting.organization,
                value.fieldLabel, importance(value), value.oldValue, value.newValue, value.sourceUrl, value.detectedAt,
                viewedAt == null || value.detectedAt.isAfter(viewedAt));
    }
    private String importance(PostingChange value) {
        return value.importance == null ? "NORMAL" : value.importance;
    }
    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

}
