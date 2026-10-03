package kr.co.jobhub.repo;

import kr.co.jobhub.model.RecruitmentPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** 공고별 채용 직렬을 조회하고 재수집 결과로 교체한다. */
public interface RecruitmentPositionRepository extends JpaRepository<RecruitmentPosition, Long> {
    List<RecruitmentPosition> findByPostingIdOrderById(Long postingId);
    @Transactional
    void deleteByPostingId(Long postingId);
}
