package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.PostingChangeCursor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostingChangeCursorRepository extends JpaRepository<PostingChangeCursor, Long> {
    Optional<PostingChangeCursor> findByUserId(Long userId);
}
