package kr.co.jobhub.community.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.community.domain.*;
import kr.co.jobhub.community.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** 공지사항과 회원별 비공개 Q&A를 제공한다. */
@RestController
@RequestMapping("/api")
public class CommunityController {
    public record NoticeRequest(@NotBlank @Size(max = 200) String title,
                                @NotBlank @Size(max = 10000) String content, boolean pinned) {}
    public record NoticeItem(Long id, String title, String content, boolean pinned,
                             Instant createdAt, Instant updatedAt) {}
    public record InquiryRequest(@NotBlank @Size(max = 30) String category,
                                 @NotBlank @Size(max = 200) String title,
                                 @NotBlank @Size(max = 10000) String content,
                                 @Size(max = 1500) String postingUrl) {}
    public record AnswerRequest(@NotBlank @Size(max = 10000) String answer) {}
    public record StatusRequest(@NotBlank String status) {}
    public record UnreadCount(long count) {}
    public record InquiryItem(Long id, String category, String title, String content, String status,
                              String postingUrl, String answer, Instant createdAt, Instant answeredAt,
                              boolean answerRead,
                              String requesterName, String requesterEmail) {}

    private final NoticeRepository notices;
    private final InquiryRepository inquiries;
    private final AppUserRepository users;

    public CommunityController(NoticeRepository notices, InquiryRepository inquiries, AppUserRepository users) {
        this.notices = notices;
        this.inquiries = inquiries;
        this.users = users;
    }

    @GetMapping("/community/notices")
    public List<NoticeItem> notices() {
        return notices.findAllByOrderByPinnedDescCreatedAtDesc().stream().map(this::notice).toList();
    }

    @PostMapping("/admin/community/notices")
    public NoticeItem createNotice(@Valid @RequestBody NoticeRequest request) {
        Notice value = new Notice();
        value.title = request.title().trim();
        value.content = request.content().trim();
        value.pinned = request.pinned();
        return notice(notices.save(value));
    }

    @PostMapping("/admin/community/notices/{id}")
    public NoticeItem updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        Notice value = notices.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        value.title = request.title().trim();
        value.content = request.content().trim();
        value.pinned = request.pinned();
        value.updatedAt = Instant.now();
        return notice(notices.save(value));
    }

    @PostMapping("/admin/community/notices/{id}/delete")
    public void deleteNotice(@PathVariable Long id) {
        if (!notices.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        notices.deleteById(id);
    }

    @GetMapping("/community/inquiries")
    public List<InquiryItem> inquiries(Principal principal, Authentication authentication) {
        AppUser user = user(principal);
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        List<Inquiry> values = admin ? inquiries.findAllByOrderByCreatedAtDesc().stream()
                .sorted(Comparator.comparingInt(value -> statusOrder(value.status))).toList()
                : inquiries.findByUserIdOrderByCreatedAtDesc(user.id);
        return values.stream().map(this::inquiry).toList();
    }

    @PostMapping("/community/inquiries")
    public InquiryItem createInquiry(@Valid @RequestBody InquiryRequest request, Principal principal) {
        Inquiry value = new Inquiry();
        value.user = user(principal);
        value.category = request.category().trim();
        value.title = request.title().trim();
        value.content = request.content().trim();
        value.postingUrl = clean(request.postingUrl());
        return inquiry(inquiries.save(value));
    }

    @PostMapping("/admin/community/inquiries/{id}/status")
    public InquiryItem status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        if (!List.of("WAITING", "IN_PROGRESS").contains(request.status()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "문의 상태를 확인해 주세요.");
        Inquiry value = inquiries.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        value.status = request.status();
        return inquiry(inquiries.save(value));
    }

    @PostMapping("/admin/community/inquiries/{id}/answer")
    public InquiryItem answer(@PathVariable Long id, @Valid @RequestBody AnswerRequest request) {
        Inquiry value = inquiries.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        value.answer = request.answer().trim();
        value.status = "ANSWERED";
        value.answeredAt = Instant.now();
        value.answerReadAt = null;
        return inquiry(inquiries.save(value));
    }

    @GetMapping("/community/inquiries/unread-count")
    public UnreadCount unreadCount(Principal principal) {
        AppUser user = user(principal);
        return new UnreadCount(inquiries.findByUserIdOrderByCreatedAtDesc(user.id).stream()
                .filter(value -> value.answer != null && value.answerReadAt == null).count());
    }

    @PostMapping("/community/inquiries/read")
    public void readAnswers(Principal principal) {
        AppUser user = user(principal);
        Instant now = Instant.now();
        List<Inquiry> unread = inquiries.findByUserIdOrderByCreatedAtDesc(user.id).stream()
                .filter(value -> value.answer != null && value.answerReadAt == null).toList();
        unread.forEach(value -> value.answerReadAt = now);
        inquiries.saveAll(unread);
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private NoticeItem notice(Notice value) {
        return new NoticeItem(value.id, value.title, value.content, value.pinned,
                value.createdAt, value.updatedAt);
    }
    private InquiryItem inquiry(Inquiry value) {
        return new InquiryItem(value.id, value.category, value.title, value.content, value.status,
                value.postingUrl, value.answer, value.createdAt, value.answeredAt,
                value.answer == null || value.answerReadAt != null,
                value.user.displayName, value.user.email);
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private int statusOrder(String status) {
        if ("WAITING".equals(status)) return 0;
        if ("IN_PROGRESS".equals(status)) return 1;
        return 2;
    }
}
