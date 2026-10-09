package kr.co.jobhub.career.domain;

import jakarta.persistence.*;
import kr.co.jobhub.auth.domain.AppUser;
import java.time.Instant;

/** 회원이 공고 변경 알림을 마지막으로 확인한 시각. */
@Entity
@Table(name = "posting_change_cursors", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class PostingChangeCursor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public AppUser user;
    @Column(name = "last_viewed_at")
    public Instant lastViewedAt;
}
