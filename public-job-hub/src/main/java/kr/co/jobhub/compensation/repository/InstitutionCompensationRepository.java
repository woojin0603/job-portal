package kr.co.jobhub.compensation.repository;

import kr.co.jobhub.compensation.domain.InstitutionCompensation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InstitutionCompensationRepository extends JpaRepository<InstitutionCompensation, Long> {
    Optional<InstitutionCompensation> findFirstByAlioInstitutionCodeOrderByFiscalYearDesc(String code);
    Optional<InstitutionCompensation> findByAlioInstitutionCodeAndFiscalYear(String code, int fiscalYear);
    List<InstitutionCompensation> findByAlioInstitutionCodeOrderByFiscalYearDesc(String code);
}
