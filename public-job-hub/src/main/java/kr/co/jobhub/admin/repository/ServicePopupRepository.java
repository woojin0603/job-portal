package kr.co.jobhub.admin.repository;

import kr.co.jobhub.admin.domain.ServicePopup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServicePopupRepository extends JpaRepository<ServicePopup, Long> {
    List<ServicePopup> findByActiveTrueOrderByCreatedAtDesc();
    List<ServicePopup> findAllByOrderByCreatedAtDesc();
}
