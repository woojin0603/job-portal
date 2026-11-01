package kr.co.jobhub.posting.repository;

import kr.co.jobhub.posting.domain.PostingChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Collection;
import java.time.Instant;

public interface PostingChangeRepository extends JpaRepository<PostingChange, Long>, JpaSpecificationExecutor<PostingChange> {
    List<PostingChange> findAllByOrderByDetectedAtDesc();
    List<PostingChange> findTop100ByPosting_IdInOrderByDetectedAtDesc(Collection<Long> postingIds);
    List<PostingChange> findTop200ByPosting_IdInAndDetectedAtAfterOrderByDetectedAtDesc(
            Collection<Long> postingIds, Instant cutoff);
    long countByDetectedAtBefore(java.time.Instant cutoff);
    long deleteByDetectedAtBefore(java.time.Instant cutoff);
}
