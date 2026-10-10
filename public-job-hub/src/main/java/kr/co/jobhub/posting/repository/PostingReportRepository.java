package kr.co.jobhub.posting.repository;
import kr.co.jobhub.posting.domain.PostingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PostingReportRepository extends JpaRepository<PostingReport,Long> {
 List<PostingReport> findAllByOrderByCreatedAtDesc();
}
