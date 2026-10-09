package kr.co.jobhub.auth.service;

import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 최초 실행 때만 기본 관리자 계정을 만들며 기존 계정의 비밀번호는 덮어쓰지 않는다. */
@Component
@ConditionalOnProperty(name = "jobhub.migration.h2-to-mysql", havingValue = "false", matchIfMissing = true)
public class DefaultAdminInitializer implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwords;
    private final String username;
    private final String password;

    public DefaultAdminInitializer(AppUserRepository users, PasswordEncoder passwords,
                                   @Value("${jobhub.admin.username:admin}") String username,
                                   @Value("${jobhub.admin.password:admin1234!}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.username = username.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank()) return;
        var existing = users.findByEmail(username);
        if (existing.isPresent()) {
            AppUser admin = existing.get();
            if ("admin1234!".equals(password) && passwords.matches(password, admin.passwordHash)
                    && !admin.mustChangePassword) {
                admin.mustChangePassword = true;
                users.save(admin);
            }
            return;
        }
        AppUser admin = new AppUser();
        admin.email = username;
        admin.passwordHash = passwords.encode(password);
        admin.displayName = "관리자";
        admin.mustChangePassword = true;
        users.save(admin);
    }
}
