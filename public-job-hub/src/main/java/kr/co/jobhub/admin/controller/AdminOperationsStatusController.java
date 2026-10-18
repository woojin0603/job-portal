package kr.co.jobhub.admin.controller;

import kr.co.jobhub.admin.service.OperationsStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자만 서버와 백업의 최근 읽기 전용 상태를 조회한다. */
@RestController
@RequestMapping("/api/admin/operations/status")
public class AdminOperationsStatusController {
    private final OperationsStatusService service;

    public AdminOperationsStatusController(OperationsStatusService service) {
        this.service = service;
    }

    @GetMapping
    public OperationsStatusService.OperationsStatus status() {
        return service.status();
    }
}
