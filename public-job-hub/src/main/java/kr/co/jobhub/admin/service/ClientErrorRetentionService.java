package kr.co.jobhub.admin.service;

import kr.co.jobhub.admin.repository.ClientErrorReportRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** 진단용 브라우저 오류가 운영 DB에 무기한 쌓이지 않도록 정리한다. */
@Service
public class ClientErrorRetentionService {
    private final ClientErrorReportRepository reports;

    public ClientErrorRetentionService(ClientErrorReportRepository reports) {
        this.reports = reports;
    }

    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        reports.deleteByResolvedTrueAndOccurredAtBefore(now.minus(Duration.ofDays(30)));
        reports.deleteByOccurredAtBefore(now.minus(Duration.ofDays(180)));
    }
}
