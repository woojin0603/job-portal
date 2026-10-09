package kr.co.jobhub.career.repository;

import kr.co.jobhub.career.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** 로그인한 회원의 스펙 프로필을 한 건씩 조회한다. */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);
}
