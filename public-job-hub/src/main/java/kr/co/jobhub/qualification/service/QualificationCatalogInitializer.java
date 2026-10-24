package kr.co.jobhub.qualification.service;

import kr.co.jobhub.qualification.domain.QualificationCatalog;
import kr.co.jobhub.qualification.repository.QualificationCatalogRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;

/** 공공기관 채용에서 자주 쓰지만 국가자격 CSV에 누락될 수 있는 공인 자격을 보완한다. */
@Component
public class QualificationCatalogInitializer implements ApplicationRunner {
    private record Seed(String name, String type, String issuer) {}
    private static final List<Seed> SEEDS = List.of(
            new Seed("한국사능력검정시험", "PUBLIC_OFFICIAL", "국사편찬위원회"),
            new Seed("빅데이터분석기사", "NATIONAL_TECHNICAL", "한국데이터산업진흥원"),
            new Seed("SQL 개발자(SQLD)", "ACCREDITED_PRIVATE", "한국데이터산업진흥원"),
            new Seed("SQL 전문가(SQLP)", "ACCREDITED_PRIVATE", "한국데이터산업진흥원"),
            new Seed("데이터분석 준전문가(ADsP)", "ACCREDITED_PRIVATE", "한국데이터산업진흥원"),
            new Seed("데이터분석 전문가(ADP)", "ACCREDITED_PRIVATE", "한국데이터산업진흥원"),
            new Seed("재경관리사", "ACCREDITED_PRIVATE", "삼일회계법인"),
            new Seed("신용분석사", "ACCREDITED_PRIVATE", "한국금융연수원")
    );
    private final QualificationCatalogRepository qualifications;
    public QualificationCatalogInitializer(QualificationCatalogRepository qualifications) {
        this.qualifications = qualifications;
    }
    @Override public void run(ApplicationArguments args) {
        for (Seed seed : SEEDS) {
            QualificationCatalog item = qualifications.findByTypeAndNameAndIssuer(seed.type, seed.name, seed.issuer)
                    .orElseGet(QualificationCatalog::new);
            item.name = seed.name; item.type = seed.type; item.issuer = seed.issuer;
            item.source = "공식 시행기관 기준 보완 목록"; item.active = true; item.sourceUpdatedAt = Instant.now();
            qualifications.save(item);
        }
    }
}