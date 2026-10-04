package kr.co.jobhub;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** 관리자가 예약 시간을 기다리지 않고 수집을 시작하고 진행 상태를 확인하는 API다. */
@RestController
@RequestMapping("/api/admin/crawl")
public class AdminCrawlController {
    public record CrawlView(boolean running, CrawlService.Status status) {}

    private final CrawlService crawler;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AdminCrawlController(CrawlService crawler) {
        this.crawler = crawler;
    }

    @GetMapping
    public CrawlView status() {
        return new CrawlView(running.get(), crawler.status());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CrawlView start() {
        if (!running.compareAndSet(false, true)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 공고를 수집하고 있습니다.");
        }
        CompletableFuture.runAsync(() -> {
            try {
                crawler.crawl();
            } finally {
                running.set(false);
            }
        });
        return new CrawlView(true, crawler.status());
    }
}
