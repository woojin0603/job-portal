package kr.co.jobhub;

import kr.co.jobhub.posting.domain.JobPosting;
import kr.co.jobhub.posting.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 실제 HTTP 보안 필터와 JPA 저장소를 함께 검증하는 통합 테스트다.
 * 인메모리 DB를 사용해 사용자 데이터와 실제 로컬 DB 파일을 분리한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jobhubtest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jobhub.crawl.enabled=false"
})
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class AuthAndScrapTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    JobPostingRepository postings;

    /** 각 테스트에서 사용할 공고를 한 번만 준비한다. */
    @BeforeEach
    void posting() {
        if (postings.findBySourceAndSourceId("TEST", "test-1").isEmpty()) {
            JobPosting p = new JobPosting();
            p.source = "TEST";
            p.sourceId = "test-1";
            p.title = "연구원 채용";
            p.organization = "테스트기관";
            p.sourceUrl = "https://job.alio.go.kr/recruit.do";
            postings.save(p);
        }
    }

    /** 가입·로그인·CSRF 거부와 서로 다른 두 회원의 스크랩 분리를 확인한다. */
    @Test
    void registrationAndScrapsBelongToSignedInUser() throws Exception {
        String body = "{\"email\":\"first@example.com\",\"password\":\"password123\",\"displayName\":\"첫 사용자\"}";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", "first@example.com").param("password", "password123"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", "first@example.com").param("password", "wrong-password"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"second@example.com\",\"password\":\"password123\",\"displayName\":\"둘째 사용자\"}"))
                .andExpect(status().isCreated());
        long id = postings.findBySourceAndSourceId("TEST", "test-1").orElseThrow().id;
        mvc.perform(post("/api/postings/{id}/scrap", id).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/postings/{id}/scrap", id).with(user("first@example.com")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/postings/{id}/scrap", id).with(user("first@example.com")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scrapped").value(true));
        mvc.perform(get("/api/postings").with(user("first@example.com")))
                .andExpect(jsonPath("$.items[0].scrapped").value(true));
        mvc.perform(get("/api/postings").with(user("second@example.com")))
                .andExpect(jsonPath("$.items[0].scrapped").value(false));

        String profile = "{\"birthDate\":\"1995-05-10\"," +
                "\"certifications\":[{\"name\":\"정보처리기사\",\"acquiredMonth\":\"2025-06\"}]," +
                "\"activities\":[{\"name\":\"공공데이터 프로젝트\",\"description\":\"기획\",\"startMonth\":\"2024-01\",\"endMonth\":\"2024-06\"}]," +
                "\"careers\":[{\"companyName\":\"테스트기관\",\"position\":\"인턴\",\"duties\":\"개발\",\"startMonth\":\"2024-07\",\"endMonth\":\"2024-12\"}]," +
                "\"degrees\":[{\"schoolName\":\"테스트대학교\",\"major\":\"컴퓨터공학\",\"degreeType\":\"학사\",\"startMonth\":\"2020-03\",\"endMonth\":\"2024-02\",\"status\":\"졸업\"}]," +
                "\"grades\":\"3.8 / 4.5\"}";
        mvc.perform(post("/api/profile").with(user("first@example.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(profile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certifications[0].name").value("정보처리기사"));
        mvc.perform(get("/api/profile").with(user("first@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degrees[0].major").value("컴퓨터공학"));
        mvc.perform(post("/api/profile").with(user("first@example.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"careers\":[{\"duties\":\"연락처 010-1234-5678\"}]}"))
                .andExpect(status().isBadRequest());
    }
}
