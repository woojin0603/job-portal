package kr.co.jobhub;

import kr.co.jobhub.model.JobPosting;
import kr.co.jobhub.repo.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    }
}
