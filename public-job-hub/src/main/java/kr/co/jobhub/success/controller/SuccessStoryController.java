package kr.co.jobhub.success.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.success.domain.SuccessStory;
import kr.co.jobhub.success.service.SuccessStoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/success-stories")
public class SuccessStoryController {
    public record SaveRequest(@NotNull Long postingId, @NotBlank @Size(max = 120) String title,
                              @NotBlank @Size(max = 5000) String content, boolean shareProfile) {}
    public record Item(Long id, Long postingId, String organization, String postingTitle, String title,
                       String content, String sharedProfile, Instant createdAt, boolean mine) {}
    private final SuccessStoryService service;
    public SuccessStoryController(SuccessStoryService service) { this.service = service; }

    @GetMapping
    public List<Item> list(Principal principal) {
        String email = principal == null ? null : principal.getName();
        return service.list().stream().map(value -> item(value, email)).toList();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Item save(@Valid @RequestBody SaveRequest request, Principal principal) {
        return item(service.save(request.postingId(), request.title(), request.content(), request.shareProfile(), principal), principal.getName());
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) { service.delete(id, principal); }

    private Item item(SuccessStory value, String email) {
        return new Item(value.id, value.posting.id, value.posting.organization, value.posting.title,
                value.title, value.content, value.sharedProfile, value.createdAt,
                email != null && email.equalsIgnoreCase(value.user.email));
    }
}
