package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.FavoriteOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteOrganizationRepository extends JpaRepository<FavoriteOrganization, Long> {
    List<FavoriteOrganization> findByUserIdOrderByOrganizationName(Long userId);
    Optional<FavoriteOrganization> findByUserIdAndNormalizedName(Long userId, String normalizedName);
    @Query("select value from FavoriteOrganization value join fetch value.user where value.user.id in :userIds")
    List<FavoriteOrganization> findWithUserByUserIdIn(@Param("userIds") Collection<Long> userIds);
}
