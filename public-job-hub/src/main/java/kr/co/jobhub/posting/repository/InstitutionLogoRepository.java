package kr.co.jobhub.posting.repository;

import kr.co.jobhub.posting.domain.InstitutionLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InstitutionLogoRepository extends JpaRepository<InstitutionLogo, Long> {
    Optional<InstitutionLogo> findByOrganizationIgnoreCase(String organization);
}
