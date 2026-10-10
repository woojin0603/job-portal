package kr.co.jobhub.auth.domain;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="login_histories",indexes=@Index(name="idx_login_email",columnList="email,created_at"))
public class LoginHistory { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=255) public String email; @Column(nullable=false) public boolean success;
 @Column(name="ip_address",length=80) public String ipAddress; @Column(name="user_agent",length=500) public String userAgent;
 @Column(name="created_at",nullable=false) public Instant createdAt=Instant.now(); }
