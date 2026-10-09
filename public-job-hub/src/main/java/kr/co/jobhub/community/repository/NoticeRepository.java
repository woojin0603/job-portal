package kr.co.jobhub.community.repository;

import kr.co.jobhub.community.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();
}
