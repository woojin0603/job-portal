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
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
public class ServicePopupController {
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final Set<String> ATTACHMENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp", "application/pdf", "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword", "application/x-hwp", "application/haansofthwp");
    public record PopupRequest(@NotBlank @Size(max=160) String title,
                               @NotBlank @Size(max=5000) String content,
                               @Size(max=1_000_000) String imageData,
                               @Size(max=255) String attachmentName,
                               @Size(max=120) String attachmentType,
                               @Size(max=1_500_000) String attachmentData,
                               boolean active) {}
    public record PopupView(Long id, String title, String content, String imageData,
                            String attachmentName, String attachmentType, String attachmentData,
                            boolean active, Instant createdAt, Instant updatedAt) {}
    public record PopupSummary(Long id, String title, boolean active, Instant createdAt, Instant updatedAt) {}
    private final ServicePopupRepository popups;
    public ServicePopupController(ServicePopupRepository popups) { this.popups = popups; }

    @GetMapping("/api/service-popups")
    public List<PopupSummary> active() { return popups.findByActiveTrueOrderByCreatedAtDesc().stream()
            .map(value -> new PopupSummary(value.id,value.title,value.active,value.createdAt,value.updatedAt)).toList(); }
    @GetMapping("/api/service-popups/{id}")
    public PopupView activeDetail(@PathVariable Long id) {
        ServicePopup value=find(id); if(!value.active) throw new ResponseStatusException(HttpStatus.NOT_FOUND); return view(value);
    }
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
        value.imageData=validatedDataUrl(request.imageData(), IMAGE_TYPES, 750_000, "본문 이미지");
        value.attachmentName=clean(request.attachmentName()); value.attachmentType=clean(request.attachmentType());
        value.attachmentData=validatedDataUrl(request.attachmentData(), ATTACHMENT_TYPES, 1_100_000, "첨부파일");
        if (value.attachmentData != null) {
            String declared = value.attachmentType == null ? "" : value.attachmentType.toLowerCase(Locale.ROOT);
            String embedded = dataType(value.attachmentData);
            if (!declared.isBlank() && !declared.equals(embedded))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "첨부파일 형식이 일치하지 않습니다.");
            value.attachmentType = embedded;
        } else { value.attachmentName = null; value.attachmentType = null; }
        value.active=request.active(); value.updatedAt=Instant.now();
    }
    private String validatedDataUrl(String value, Set<String> allowedTypes, int maximumBytes, String label) {
        if (value == null || value.isBlank()) return null;
        String type = dataType(value);
        if (!allowedTypes.contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " 형식을 확인해 주세요.");
        try {
            byte[] decoded = Base64.getDecoder().decode(value.substring(value.indexOf(',') + 1));
            if (decoded.length == 0 || decoded.length > maximumBytes || !matchesSignature(type, decoded))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " 내용 또는 크기를 확인해 주세요.");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " 인코딩이 올바르지 않습니다.");
        }
        return value;
    }
    private String dataType(String value) {
        if (!value.startsWith("data:") || !value.contains(";base64,"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Base64 파일 데이터만 사용할 수 있습니다.");
        return value.substring(5, value.indexOf(";base64,")).toLowerCase(Locale.ROOT);
    }
    private boolean matchesSignature(String type, byte[] bytes) {
        if (type.equals("text/plain")) return true;
        if (type.equals("image/png")) return starts(bytes, 0x89, 0x50, 0x4e, 0x47);
        if (type.equals("image/jpeg")) return starts(bytes, 0xff, 0xd8, 0xff);
        if (type.equals("image/webp")) return bytes.length >= 12 && starts(bytes, 0x52,0x49,0x46,0x46)
                && bytes[8]=='W' && bytes[9]=='E' && bytes[10]=='B' && bytes[11]=='P';
        if (type.equals("application/pdf")) return starts(bytes, 0x25,0x50,0x44,0x46);
        if (type.contains("openxmlformats")) return starts(bytes, 0x50,0x4b,0x03,0x04);
        if (type.equals("application/msword") || type.contains("hwp"))
            return starts(bytes, 0xd0,0xcf,0x11,0xe0,0xa1,0xb1,0x1a,0xe1);
        return false;
    }
    private boolean starts(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) return false;
        for (int index=0; index<signature.length; index++) if ((bytes[index]&255)!=signature[index]) return false;
        return true;
    }
    private String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private PopupView view(ServicePopup value) { return new PopupView(value.id,value.title,value.content,value.imageData,
            value.attachmentName,value.attachmentType,value.attachmentData,value.active,value.createdAt,value.updatedAt); }
}
