package kr.co.jobhub.auth.controller;
import jakarta.servlet.http.HttpSession; import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import kr.co.jobhub.auth.domain.*; import kr.co.jobhub.auth.repository.*; import kr.co.jobhub.auth.service.TotpService;
import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException;
import java.net.URLEncoder; import java.nio.charset.StandardCharsets; import java.security.Principal; import java.time.Instant; import java.util.List;
@RestController @RequestMapping("/api/auth/security") public class AccountSecurityController {
 public record History(boolean success,String ipAddress,String userAgent,Instant createdAt){} public record View(boolean twoFactorEnabled,boolean twoFactorVerified,List<History> loginHistory){}
 public record CodeRequest(@NotBlank @Pattern(regexp="\\d{6}") String code){} public record DisableRequest(@NotBlank String password,@NotBlank String code){}
 public record Setup(String secret,String otpauthUri){}
 private final AppUserRepository users;private final LoginHistoryRepository histories;private final TotpService totp;private final PasswordEncoder passwords;
 public AccountSecurityController(AppUserRepository u,LoginHistoryRepository h,TotpService t,PasswordEncoder p){users=u;histories=h;totp=t;passwords=p;}
 @GetMapping public View view(Principal p,HttpSession s){AppUser u=user(p);return new View(Boolean.TRUE.equals(u.twoFactorEnabled),Boolean.TRUE.equals(s.getAttribute("ADMIN_2FA_VERIFIED")),histories.findTop20ByEmailOrderByCreatedAtDesc(u.email).stream().map(v->new History(v.success,v.ipAddress,v.userAgent,v.createdAt)).toList());}
 @PostMapping("/logout-others") public void logoutOthers(Principal p,HttpSession s){AppUser u=user(p);u.sessionVersion=(u.sessionVersion==null?0L:u.sessionVersion)+1;users.save(u);s.setAttribute("SESSION_VERSION",u.sessionVersion);}
 @PostMapping("/2fa/setup") public Setup setup(Principal p){AppUser u=admin(p);u.twoFactorSecret=totp.secret();users.save(u);String label=URLEncoder.encode("JOB HUB KOREA:"+u.email,StandardCharsets.UTF_8);return new Setup(u.twoFactorSecret,"otpauth://totp/"+label+"?secret="+u.twoFactorSecret+"&issuer=JOB%20HUB%20KOREA");}
 @PostMapping("/2fa/enable") public void enable(@Valid @RequestBody CodeRequest r,Principal p,HttpSession s){AppUser u=admin(p);if(!totp.verify(u.twoFactorSecret,r.code()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"인증 코드가 올바르지 않습니다.");u.twoFactorEnabled=true;users.save(u);s.setAttribute("ADMIN_2FA_VERIFIED",true);}
 @PostMapping("/2fa/verify") public void verify(@Valid @RequestBody CodeRequest r,Principal p,HttpSession s){AppUser u=admin(p);if(!Boolean.TRUE.equals(u.twoFactorEnabled)||!totp.verify(u.twoFactorSecret,r.code()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"인증 코드가 올바르지 않습니다.");s.setAttribute("ADMIN_2FA_VERIFIED",true);}
 @PostMapping("/2fa/disable") public void disable(@Valid @RequestBody DisableRequest r,Principal p,HttpSession s){AppUser u=admin(p);if(!passwords.matches(r.password(),u.passwordHash)||!totp.verify(u.twoFactorSecret,r.code()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"비밀번호 또는 인증 코드를 확인해 주세요.");u.twoFactorEnabled=false;u.twoFactorSecret=null;users.save(u);s.removeAttribute("ADMIN_2FA_VERIFIED");}
 private AppUser admin(Principal p){if(!(p instanceof Authentication a)||a.getAuthorities().stream().noneMatch(v->"ROLE_ADMIN".equals(v.getAuthority())))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return user(p);}
 private AppUser user(Principal p){if(p==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return users.findByEmail(p.getName()).orElseThrow();}
}
