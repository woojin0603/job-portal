package kr.co.jobhub.auth.repository;
import kr.co.jobhub.auth.domain.LoginHistory; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface LoginHistoryRepository extends JpaRepository<LoginHistory,Long>{List<LoginHistory> findTop20ByEmailOrderByCreatedAtDesc(String email);}
