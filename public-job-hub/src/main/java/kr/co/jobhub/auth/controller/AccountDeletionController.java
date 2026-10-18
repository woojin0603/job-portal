package kr.co.jobhub.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kr.co.jobhub.auth.service.AccountDeletionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account")
public class AccountDeletionController {
    public record DeleteRequest(@NotBlank String password, @NotBlank String confirmation) {}
    private final AccountDeletionService deletion;

    public AccountDeletionController(AccountDeletionService deletion) { this.deletion = deletion; }

    @PostMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Valid @RequestBody DeleteRequest request, Authentication auth,
                       HttpServletRequest servletRequest) {
        if (auth == null || !auth.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (auth.getAuthorities().stream().anyMatch(value -> "ROLE_ADMIN".equals(value.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 계정은 웹에서 탈퇴할 수 없습니다.");
        deletion.delete(auth.getName(), request.password(), request.confirmation());
        HttpSession session = servletRequest.getSession(false);
        if (session != null) session.invalidate();
    }
}
