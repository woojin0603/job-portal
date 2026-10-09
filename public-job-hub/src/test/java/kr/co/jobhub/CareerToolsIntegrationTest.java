package kr.co.jobhub;

import kr.co.jobhub.compensation.domain.InstitutionCompensation;
import kr.co.jobhub.compensation.repository.InstitutionCompensationRepository;
import kr.co.jobhub.posting.domain.JobPosting;
import kr.co.jobhub.posting.domain.RecruitmentPosition;
import kr.co.jobhub.posting.repository.JobPostingRepository;
import kr.co.jobhub.posting.repository.RecruitmentPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 지역·직렬 검색부터 개인 지원현황과 맞춤 알림까지 핵심 사용자 흐름을 검증한다. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:careertoolstest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jobhub.crawl.enabled=false"
})
@AutoConfigureMockMvc
class CareerToolsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JobPostingRepository postings;
    @Autowired RecruitmentPositionRepository positions;
    @Autowired InstitutionCompensationRepository compensations;

    private long postingId;

    @BeforeEach
    void preparePostingAndUser() throws Exception {
        JobPosting posting = postings.findBySourceAndSourceId("TEST", "career-tools-1").orElseGet(() -> {
            JobPosting value = new JobPosting();
            value.source = "TEST";
            value.sourceId = "career-tools-1";
            value.title = "수원 전산직 채용";
            value.organization = "테스트 공공기관";
            value.alioInstitutionCode = "TEST001";
            value.region = "경기도 수원시";
            value.employmentType = "정규직";
            value.organizationType = "PUBLIC";
            value.mobilityType = "FIXED";
            value.postedAt = LocalDate.now().minusDays(1);
            value.deadline = LocalDate.now().plusDays(7);
            value.sourceUrl = "https://job.alio.go.kr/recruit.do";
            return postings.save(value);
        });
        postingId = posting.id;
        posting.alioInstitutionCode = "TEST001";
        postings.save(posting);
        if (positions.findByPostingIdOrderById(posting.id).isEmpty()) {
            RecruitmentPosition position = new RecruitmentPosition();
            position.posting = posting;
            position.standardCategory = "전산·IT";
            position.originalName = "전산직";
            position.headcount = 2;
            position.workRegion = "수원시";
            positions.save(position);
        }
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"tools@example.com\",\"password\":\"password123\",\"displayName\":\"도구 사용자\"}"));
    }

    @Test
    void resolvesLatestSalaryByAlioInstitutionCode() throws Exception {
        InstitutionCompensation value = compensations
                .findByAlioInstitutionCodeAndFiscalYear("TEST001", 2025)
                .orElseGet(InstitutionCompensation::new);
        value.alioInstitutionCode = "TEST001";
        value.organization = "테스트 공공기관";
        value.fiscalYear = 2025;
        value.totalAmount = 42000L;
        compensations.save(value);

        mvc.perform(get("/api/postings/{id}/salary", postingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.alioInstitutionCode").value("TEST001"))
                .andExpect(jsonPath("$.fiscalYear").value(2025))
                .andExpect(jsonPath("$.totalAmount").value(42000));
    }

    @Test
    void filtersPostingByProvinceDistrictAndJobCategory() throws Exception {
        mvc.perform(get("/api/postings").with(user("tools@example.com"))
                        .param("region", "경기").param("regionFull", "경기도")
                        .param("district", "수원시").param("jobCategory", "전산·IT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].publicInstitutionType").value("LOCAL_PUBLIC"))
                .andExpect(jsonPath("$.items[0].positions[0].originalName").value("전산직"));

        mvc.perform(get("/api/postings").param("region", "경기").param("regionFull", "경기도")
                        .param("district", "성남시"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void savesApplicationStageAndReturnsTailoredAlerts() throws Exception {
        mvc.perform(post("/api/postings/{id}/scrap", postingId)
                        .with(user("tools@example.com")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scrapped").value(true));
        mvc.perform(post("/api/tools/postings/{id}/application", postingId)
                        .with(user("tools@example.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"INTERVIEW\",\"nextStepDate\":\"2030-01-10\",\"memo\":\"면접 준비\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("INTERVIEW"));

        mvc.perform(post("/api/tools/preferences").with(user("tools@example.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keywords\":\"전산\",\"regions\":\"수원\",\"mobilityTypes\":\"FIXED\",\"enabled\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tools/alerts").with(user("tools@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(postingId))
                .andExpect(jsonPath("$[0].fresh").value(true));
    }
}
