package kr.co.jobhub.posting.repository;

import kr.co.jobhub.posting.domain.RecruitmentCompetition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface RecruitmentCompetitionRepository extends JpaRepository<RecruitmentCompetition, Long> {
    List<RecruitmentCompetition> findByPostingIdOrderById(Long postingId);
    boolean existsByPostingId(Long postingId);

    @Transactional
    void deleteByPostingId(Long postingId);
}
