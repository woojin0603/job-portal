package kr.co.jobhub.posting.domain;
import jakarta.persistence.*;
import kr.co.jobhub.auth.domain.AppUser;
import java.time.Instant;
@Entity @Table(name="posting_reports", indexes=@Index(name="idx_report_status", columnList="status,created_at"))
public class PostingReport {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="posting_id") public JobPosting posting;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") public AppUser user;
 @Column(nullable=false,length=40) public String category;
 @Column(nullable=false,length=2000) public String content;
 @Column(nullable=false,length=20) public String status="RECEIVED";
 @Column(name="admin_note",length=2000) public String adminNote;
 @Column(name="created_at",nullable=false) public Instant createdAt=Instant.now();
 @Column(name="resolved_at") public Instant resolvedAt;
}
