package kr.co.jobhub.repo;

import kr.co.jobhub.model.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** 채용공고 검색과 수집 시 중복 확인을 담당하는 JPA 저장소. */
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    /** 수집 출처와 원본 ID 조합으로 이미 저장한 공고를 찾는다. */
    Optional<JobPosting> findBySourceAndSourceId(String source, String sourceId);

    /**
     * 공개 중인 공고를 제목·기관명으로 검색하고 20건씩 페이지로 나눈다.
     * mine이 참이면 지정 회원이 스크랩한 공고만 DB 쿼리 단계에서 필터링한다.
     */
    @Query("select p from JobPosting p where p.open = true and " +
           "(lower(p.title) like lower(concat('%', :q, '%')) or lower(p.organization) like lower(concat('%', :q, '%'))) " +
           "and (:mine = false or exists (select s.id from Scrap s where s.posting = p and s.user.id = :userId))")
    Page<JobPosting> search(@Param("q") String q, @Param("mine") boolean mine,
                            @Param("userId") Long userId, Pageable pageable);
}
