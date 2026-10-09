package kr.co.jobhub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.model.*;
import kr.co.jobhub.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
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
                                 @NotBlank @Size(max = 10000) String content) {}
    public record AnswerRequest(@NotBlank @Size(max = 10000) String answer) {}
    public record InquiryItem(Long id, String category, String title, String content, String status,
                              String answer, Instant createdAt, Instant answeredAt,
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

    @GetMapping("/community/inquiries")
    public List<InquiryItem> inquiries(Principal principal, Authentication authentication) {
        AppUser user = user(principal);
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        List<Inquiry> values = admin ? inquiries.findAllByOrderByCreatedAtDesc()
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
        return inquiry(inquiries.save(value));
    }

    @PostMapping("/admin/community/inquiries/{id}/answer")
    public InquiryItem answer(@PathVariable Long id, @Valid @RequestBody AnswerRequest request) {
        Inquiry value = inquiries.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        value.answer = request.answer().trim();
        value.status = "ANSWERED";
        value.answeredAt = Instant.now();
        return inquiry(inquiries.save(value));
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
                value.answer, value.createdAt, value.answeredAt,
                value.user.displayName, value.user.email);
    }
}
