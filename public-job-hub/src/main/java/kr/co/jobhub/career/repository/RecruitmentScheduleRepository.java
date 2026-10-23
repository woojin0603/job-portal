package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.RecruitmentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

public interface RecruitmentScheduleRepository extends JpaRepository<RecruitmentSchedule, Long> {
    List<RecruitmentSchedule> findByPostingIdOrderByEventDateAsc(Long postingId);
    List<RecruitmentSchedule> findByPostingIdInAndEventDateGreaterThanEqualOrderByEventDateAsc(List<Long> postingIds, LocalDate date);
    @Transactional
    void deleteByPostingId(Long postingId);
}
