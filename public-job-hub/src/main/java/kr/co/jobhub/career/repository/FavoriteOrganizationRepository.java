package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.FavoriteOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteOrganizationRepository extends JpaRepository<FavoriteOrganization, Long> {
    List<FavoriteOrganization> findByUserIdOrderByOrganizationName(Long userId);
    Optional<FavoriteOrganization> findByUserIdAndNormalizedName(Long userId, String normalizedName);
}
