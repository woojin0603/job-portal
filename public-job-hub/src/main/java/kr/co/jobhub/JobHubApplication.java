package kr.co.jobhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 공공기관 채용공고 서비스의 Spring Boot 시작점이다.
 * 웹 API, JPA 저장소, 인증 설정을 스캔하고 예약 수집 작업을 활성화한다.
 */
@SpringBootApplication
@EnableScheduling
public class JobHubApplication {
    /** 애플리케이션 컨텍스트와 내장 웹 서버를 시작한다. */
    public static void main(String[] args) {
        SpringApplication.run(JobHubApplication.class, args);
    }
}
