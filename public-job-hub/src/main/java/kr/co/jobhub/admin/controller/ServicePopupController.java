package kr.co.jobhub.admin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.admin.domain.ServicePopup;
import kr.co.jobhub.admin.repository.ServicePopupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@RestController
public class ServicePopupController {
    public record PopupRequest(@NotBlank @Size(max=160) String title,
                               @NotBlank @Size(max=5000) String content,
                               @Size(max=1_000_000) String imageData,
                               boolean active) {}
    public record PopupView(Long id, String title, String content, String imageData,
                            boolean active, Instant createdAt, Instant updatedAt) {}
    private final ServicePopupRepository popups;
    public ServicePopupController(ServicePopupRepository popups) { this.popups = popups; }

    @GetMapping("/api/service-popups")
    public List<PopupView> active() { return popups.findByActiveTrueOrderByCreatedAtDesc().stream().map(this::view).toList(); }
    @GetMapping("/api/admin/service-popups")
    public List<PopupView> all() { return popups.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList(); }
    @PostMapping("/api/admin/service-popups")
    public PopupView create(@Valid @RequestBody PopupRequest request) {
        ServicePopup value = new ServicePopup(); apply(value, request); return view(popups.save(value));
    }
    @PutMapping("/api/admin/service-popups/{id}")
    public PopupView update(@PathVariable Long id, @Valid @RequestBody PopupRequest request) {
        ServicePopup value = find(id); apply(value, request); return view(popups.save(value));
    }
    @PostMapping("/api/admin/service-popups/{id}")
    public PopupView updateByPost(@PathVariable Long id, @Valid @RequestBody PopupRequest request) {
        return update(id, request);
    }
    @PostMapping("/api/admin/service-popups/{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByPost(@PathVariable Long id) { delete(id); }
    @DeleteMapping("/api/admin/service-popups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { popups.delete(find(id)); }
    private ServicePopup find(Long id) { return popups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private void apply(ServicePopup value, PopupRequest request) {
        value.title=request.title().trim(); value.content=request.content().trim();
        value.imageData=request.imageData()==null||request.imageData().isBlank()?null:request.imageData();
        value.active=request.active(); value.updatedAt=Instant.now();
    }
    private PopupView view(ServicePopup value) { return new PopupView(value.id,value.title,value.content,value.imageData,value.active,value.createdAt,value.updatedAt); }
}
