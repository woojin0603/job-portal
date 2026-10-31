package kr.co.jobhub.admin.repository;

import kr.co.jobhub.admin.domain.ClientErrorReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface ClientErrorReportRepository extends JpaRepository<ClientErrorReport, Long> {
    Page<ClientErrorReport> findByResolvedFalse(Pageable pageable);

    @Transactional
    long deleteByResolvedTrueAndOccurredAtBefore(Instant cutoff);

    @Transactional
    long deleteByOccurredAtBefore(Instant cutoff);
}
