package kr.co.jobhub.posting.service;

import kr.co.jobhub.posting.domain.InstitutionLogo;
import kr.co.jobhub.posting.repository.InstitutionLogoRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 자동 요청을 제한하는 주요 기관의 검증된 공식 로고를 최초 한 번 등록한다. */
@Component
public class InstitutionLogoInitializer implements ApplicationRunner {
    private final InstitutionLogoRepository logos;

    public InstitutionLogoInitializer(InstitutionLogoRepository logos) {
        this.logos = logos;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (logos.findByOrganizationIgnoreCase("한전KDN").isPresent()) return;
        InstitutionLogo logo = new InstitutionLogo();
        logo.organization = "한전KDN";
        logo.logoUrl = "https://www.kdn.com/korean/img/common/h1_logo.png";
        logos.save(logo);
    }
}
