package kr.co.jobhub.auth.repository;

import kr.co.jobhub.auth.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** 회원의 이메일 조회와 중복 검사에 사용하는 JPA 저장소. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    /** 로그인 정보 조회 또는 현재 세션의 회원 정보 복원에 사용한다. */
    Optional<AppUser> findByEmail(String email);

    /** 회원가입 시 동일한 이메일이 이미 등록되어 있는지 확인한다. */
    boolean existsByEmail(String email);

    /** 지연 로딩 프록시를 거치지 않고 비밀번호와 세션 버전을 DB에서 직접 갱신한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AppUser user
               set user.passwordHash = :passwordHash,
                   user.mustChangePassword = false,
                   user.sessionVersion = coalesce(user.sessionVersion, 0) + 1
             where user.id = :userId
            """)
    int resetPassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
}
