package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** 회원별 공고 보관 상태를 조회하는 JPA 저장소. */
public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    /** 목록 카드에 스크랩·지원 상태를 표시하기 위해 회원의 스크랩을 읽는다. */
    List<Scrap> findByUserId(Long userId);

    /** 스크랩 토글과 지원 완료 변경 시 단일 관계를 찾는다. */
    Optional<Scrap> findByUserIdAndPostingId(Long userId, Long postingId);
}
