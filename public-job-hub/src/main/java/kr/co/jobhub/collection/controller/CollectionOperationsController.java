package kr.co.jobhub.collection.controller;

import kr.co.jobhub.collection.service.CrawlService;
import kr.co.jobhub.collection.service.PublicRecruitmentApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** 서버 내부 자동 점검이 최근 공고 수집의 성공 여부를 확인하는 최소 공개 상태 API다. */
@RestController
@RequestMapping("/api/operations/collection-status")
public class CollectionOperationsController {
    public record CollectionHealth(boolean healthy,
                                   Instant crawlLastAttempt, boolean crawlFailed,
                                   Instant apiLastAttempt, boolean apiFailed) {}

    private final CrawlService crawler;
    private final PublicRecruitmentApiService publicApi;

    public CollectionOperationsController(CrawlService crawler, PublicRecruitmentApiService publicApi) {
        this.crawler = crawler;
        this.publicApi = publicApi;
    }

    @GetMapping
    public CollectionHealth status() {
        CrawlService.Status crawl = crawler.status();
        PublicRecruitmentApiService.Status api = publicApi.status();
        boolean crawlFailed = crawl.lastAttempt() != null && crawl.error() != null;
        boolean apiFailed = api.lastAttempt() != null && api.error() != null;
        return new CollectionHealth(!crawlFailed && !apiFailed,
                crawl.lastAttempt(), crawlFailed, api.lastAttempt(), apiFailed);
    }
}
