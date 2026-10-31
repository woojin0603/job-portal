package kr.co.jobhub.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.admin.domain.ClientErrorReport;
import kr.co.jobhub.admin.repository.ClientErrorReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 브라우저 오류 접수와 관리자 진단 목록을 제공한다. */
@RestController
public class ClientErrorReportController {
    public record ErrorRequest(@NotBlank @Size(max = 30) String scope,
                               @NotBlank @Size(max = 500) String message,
                               @Size(max = 500) String path,
                               @Size(max = 500) String userAgent,
                               @Size(max = 8000) String stack,
                               @Size(max = 8000) String componentStack) {}
    public record ErrorView(Long id, String scope, String message, String path, String userAgent,
                            String account, String stack, String componentStack,
                            Instant occurredAt, boolean resolved) {}

    private static final Map<String, ArrayDeque<Instant>> REQUESTS = new ConcurrentHashMap<>();
    private final ClientErrorReportRepository reports;

    public ClientErrorReportController(ClientErrorReportRepository reports) {
        this.reports = reports;
    }

    @PostMapping("/api/client-errors")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.ACCEPTED)
    public void report(@Valid @RequestBody ErrorRequest request,
                       HttpServletRequest servletRequest,
                       Principal principal) {
        requireRateLimit(servletRequest.getRemoteAddr());
        ClientErrorReport value = new ClientErrorReport();
        value.scope = request.scope();
        value.message = request.message();
        value.path = request.path();
        value.userAgent = request.userAgent();
        // Whether the error happened after sign-in is useful, but an email address
        // is not required for diagnostics and must not be retained here.
        value.account = principal == null ? null : "authenticated";
        value.stack = request.stack();
        value.componentStack = request.componentStack();
        reports.save(value);
    }

    @GetMapping("/api/admin/client-errors")
    public List<ErrorView> list() {
        return reports.findByResolvedFalse(PageRequest.of(0, 100,
                        Sort.by(Sort.Direction.DESC, "occurredAt"))).stream()
                .map(this::view)
                .toList();
    }

    @PostMapping("/api/admin/client-errors/{id}/resolve")
    public ErrorView resolve(@PathVariable Long id) {
        ClientErrorReport value = reports.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        value.resolved = true;
        value.resolvedAt = Instant.now();
        return view(reports.save(value));
    }

    private ErrorView view(ClientErrorReport value) {
        return new ErrorView(value.id, value.scope, value.message, value.path, value.userAgent,
                value.account, value.stack, value.componentStack, value.occurredAt, value.resolved);
    }

    private void requireRateLimit(String source) {
        if (REQUESTS.size() > 10_000) REQUESTS.clear();
        Instant cutoff = Instant.now().minusSeconds(60);
        ArrayDeque<Instant> values = REQUESTS.computeIfAbsent(source == null ? "unknown" : source,
                ignored -> new ArrayDeque<>());
        synchronized (values) {
            while (!values.isEmpty() && values.peekFirst().isBefore(cutoff)) values.removeFirst();
            if (values.size() >= 10) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
            values.addLast(Instant.now());
        }
    }

}
