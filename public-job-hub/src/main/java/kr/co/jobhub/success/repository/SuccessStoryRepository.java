package kr.co.jobhub.success.repository;

import kr.co.jobhub.success.domain.SuccessStory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SuccessStoryRepository extends JpaRepository<SuccessStory, Long> {
    List<SuccessStory> findAllByOrderByCreatedAtDesc();
    List<SuccessStory> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SuccessStory> findByUserIdAndScrapId(Long userId, Long scrapId);
}
