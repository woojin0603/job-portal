package kr.co.jobhub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 자정 예약과 별개로 운영자가 명시적으로 요청했을 때 한 번 수집하는 실행 모드다.
 * --jobhub.crawl.run-once=true 옵션을 준 별도 프로세스에서 사용한다.
 */
@Component
public class CrawlOnceRunner implements ApplicationRunner {
    private final CrawlService crawler;
    private final ConfigurableApplicationContext context;
    private final boolean runOnce;
    private final boolean reanalyzeExisting;

    /** 수집 서비스와 실행 옵션을 주입받는다. 기본 실행에서는 아무 동작도 하지 않는다. */
    public CrawlOnceRunner(CrawlService crawler, ConfigurableApplicationContext context,
                           @Value("${jobhub.crawl.run-once:false}") boolean runOnce,
                           @Value("${jobhub.crawl.reanalyze-existing:false}") boolean reanalyzeExisting) {
        this.crawler = crawler;
        this.context = context;
        this.runOnce = runOnce;
        this.reanalyzeExisting = reanalyzeExisting;
    }

    /** 요청된 경우 즉시 수집하고 결과를 출력한 뒤 임시 서버 프로세스를 닫는다. */
    @Override
    public void run(ApplicationArguments args) {
        if (!runOnce && !reanalyzeExisting) {
            return;
        }
        if (reanalyzeExisting) crawler.reanalyzeExisting();
        else crawler.crawl();
        CrawlService.Status status = crawler.status();
        System.out.println((reanalyzeExisting ? "Existing posting reanalysis" : "Immediate crawl")
                + ": count=" + status.lastCount() + ", error=" + status.error());
        SpringApplication.exit(context);
    }
}
