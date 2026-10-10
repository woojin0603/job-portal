package kr.co.jobhub.posting.repository;

import kr.co.jobhub.posting.domain.PostingChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface PostingChangeRepository extends JpaRepository<PostingChange, Long>, JpaSpecificationExecutor<PostingChange> {
    List<PostingChange> findAllByOrderByDetectedAtDesc();
    long countByDetectedAtBefore(java.time.Instant cutoff);
    long deleteByDetectedAtBefore(java.time.Instant cutoff);
}
