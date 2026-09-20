package kr.co.jobhub.repo;

import kr.co.jobhub.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** 회원의 이메일 조회와 중복 검사에 사용하는 JPA 저장소. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    /** 로그인 정보 조회 또는 현재 세션의 회원 정보 복원에 사용한다. */
    Optional<AppUser> findByEmail(String email);

    /** 회원가입 시 동일한 이메일이 이미 등록되어 있는지 확인한다. */
    boolean existsByEmail(String email);
}
