package kr.co.jobhub.auth.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.auth.service.PasswordResetService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 계정 존재 여부를 노출하지 않는 공개 비밀번호 재설정 API다. */
@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {
    public record Request(@Email @NotBlank String email) {}
    public record Confirm(@NotBlank String token,
                          @NotBlank @Size(min = 10, max = 100) String newPassword) {}
    public record Result(boolean accepted, boolean mailEnabled) {}

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @PostMapping("/request")
    public Result request(@Valid @RequestBody Request request) {
        service.request(request.email());
        return new Result(true, service.mailEnabled());
    }

    @PostMapping("/confirm")
    public Result confirm(@Valid @RequestBody Confirm request) {
        service.reset(request.token(), request.newPassword());
        return new Result(true, service.mailEnabled());
    }
}
