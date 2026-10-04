package kr.co.jobhub;

import org.springframework.web.bind.annotation.*;

/** 관리자가 예약 시각을 기다리지 않고 공식 채용 API 동기화를 실행한다. */
@RestController
@RequestMapping("/api/admin/public-recruitment")
public class AdminPublicRecruitmentController {
    private final PublicRecruitmentApiService service;

    public AdminPublicRecruitmentController(PublicRecruitmentApiService service) { this.service = service; }

    @GetMapping
    public PublicRecruitmentApiService.Status status() { return service.status(); }

    @PostMapping
    public PublicRecruitmentApiService.Status sync() { return service.sync(); }
}
