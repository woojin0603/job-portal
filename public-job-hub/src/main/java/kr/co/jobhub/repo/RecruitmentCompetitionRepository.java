package kr.co.jobhub.repo;

import kr.co.jobhub.model.RecruitmentCompetition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface RecruitmentCompetitionRepository extends JpaRepository<RecruitmentCompetition, Long> {
    List<RecruitmentCompetition> findByPostingIdOrderById(Long postingId);
    boolean existsByPostingId(Long postingId);

    @Transactional
    void deleteByPostingId(Long postingId);
}
