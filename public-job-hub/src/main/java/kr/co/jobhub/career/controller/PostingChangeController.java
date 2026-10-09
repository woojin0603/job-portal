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

/** 스크랩 및 관심 기관 공고의 변경 알림을 회원별로 제공한다. */
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
    private final FavoriteOrganizationRepository favorites;
    private final AppUserRepository users;

    public PostingChangeController(PostingChangeRepository changes, PostingChangeCursorRepository cursors,
                                   ScrapRepository scraps, FavoriteOrganizationRepository favorites,
                                   AppUserRepository users) {
        this.changes = changes;
        this.cursors = cursors;
        this.scraps = scraps;
        this.favorites = favorites;
        this.users = users;
    }

    @GetMapping
    public List<ChangeItem> list(Principal principal) {
        AppUser user = user(principal);
        Instant viewedAt = cursors.findByUserId(user.id).map(value -> value.lastViewedAt).orElse(null);
        Set<Long> postingIds = scraps.findByUserId(user.id).stream()
                .map(value -> value.posting.id).collect(Collectors.toSet());
        Set<String> organizations = favorites.findByUserIdOrderByOrganizationName(user.id).stream()
                .map(value -> value.normalizedName).collect(Collectors.toSet());
        return changes.findAllByOrderByDetectedAtDesc().stream()
                .filter(value -> postingIds.contains(value.posting.id)
                        || organizations.contains(normalize(value.posting.organization)))
                .limit(100)
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
    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.KOREA)
                .replaceAll("^(주식회사|재단법인|사단법인|학교법인|의료법인|\\(주\\)|\\(재\\)|\\(사\\)|㈜)", "")
                .replaceAll("[^가-힣a-z0-9]", "");
    }
}
