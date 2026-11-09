# Public Job Hub 개인정보·출시 정책 감사

감사일: 2026-11-09  
범위: 회원정보, 스펙·지원관리, 문의·게시물, 로그인 기록, 푸시, 메일, 백업, 공공데이터·웹 수집, Google Play 공개 요건

이 문서는 코드와 현재 운영 구성을 대조한 개발·운영 점검 기록이며 법률 자문을 대체하지 않는다.

## 감사 결과 요약

### 코드에서 보완 완료

- 회원 탈퇴 시 `success_stories`, `push_delivery_receipts`를 포함한 회원 소유 테이블 삭제
- 합격 후기의 스크랩 외래키를 고려해 합격 후기부터 삭제
- 로그인 기록 180일, 관리자 감사 기록 365일 자동 파기
- 푸시 연결 해제 및 영구 전송 실패 시 서버의 기기 토큰 즉시 삭제
- 만료된 비밀번호 재설정 토큰 매일 삭제
- 브라우저 오류 기록은 해결 기록 30일, 전체 최대 180일로 자동 파기
- 공개 개인정보처리방침·이용약관·데이터 출처·계정 삭제 URL 제공
- 삭제 대상 테이블과 보존기간을 검증하는 단위 테스트 추가

### 공개 출시 전 차단 항목

1. 개인정보 보호책임자의 실명 또는 사업자명, 개인정보 문의 이메일을 확정해 정책에 기재해야 한다.
2. Play Console 개발자 표시명과 개인정보처리방침의 운영 주체 명칭을 동일하게 맞춰야 한다.
3. TiDB Cloud 계약상 수탁 법인·하위처리자(AWS 포함), Oracle 계약 법인, Google 하위처리자의 실제 연락처와 이전 국가 목록을 각 계약 화면에서 확인해야 한다.
4. Firebase Cloud Messaging은 글로벌 인프라에서 처리될 수 있으므로 단순히 ‘미국’으로 단정하지 말고 Google의 최신 하위처리자·처리 위치 문서와 정책을 대조해야 한다.
5. 공공데이터포털에서 실제 사용하는 각 API의 이용허락 범위, 출처표시, 상업 이용·변경 가능 여부를 데이터별로 기록해야 한다.
6. JOB-ALIO 및 개별 기관 페이지의 자동 수집은 robots.txt, 이용약관, 요청 빈도, 원문·첨부파일 재배포 범위를 출처별로 확인해야 한다.
7. 기관 로고는 정보 식별에 필요한 최소 범위로만 사용하고, 권리자 신고 시 즉시 교체·삭제할 운영 절차를 유지해야 한다.

## 실제 개인정보 흐름

| 단계 | 데이터 | 저장·전송 위치 | 보유 기준 |
| --- | --- | --- | --- |
| 가입·로그인 | 이메일, 표시 이름, 비밀번호 해시, 동의 기록 | TiDB Cloud | 탈퇴 시 삭제 |
| 보안 | IP, User-Agent, 접속 시각, 로그인 성공 여부 | TiDB Cloud | 180일 |
| 스펙·지원관리 | 학위, 성적, 자격증, 경력, 활동, 스크랩, 일정, 메모 | TiDB Cloud | 사용자 삭제·탈퇴 시 삭제 |
| 문의·후기 | 제목, 본문, 답변, 공개 프로필 스냅샷 | TiDB Cloud | 사용자 삭제·탈퇴 시 삭제 |
| 웹 푸시 | Firebase 설치 식별자 기반 토큰, 알림 제목·본문·링크 | TiDB Cloud → Firebase Cloud Messaging | 연결 해제·실패·탈퇴 시 서버 토큰 삭제; Google 백업 삭제는 공급자 정책 적용 |
| 비밀번호 재설정 | 이메일, 해시된 일회용 토큰, 재설정 링크 | TiDB Cloud → Gmail SMTP | 토큰 30분 후 정리; 메일은 수신자 메일함 정책 적용 |
| 장애 복구 | 위 DB 데이터의 암호화 덤프 | OCI 서버·Object Storage | 로컬 7일, 외부 최대 56일 |

## 탈퇴 검증 대상 테이블

`password_reset_tokens`, `posting_reports`, `inquiries`, `push_delivery_receipts`,
`push_subscriptions`, `user_notification_states`, `posting_change_cursors`,
`favorite_organizations`, `job_alert_preferences`, `success_stories`, `scraps`,
`user_profiles`, `login_histories`, `app_users`.

`admin_audit_logs`의 자기 탈퇴 기록은 계정 이메일 대신 `deleted-user`로 남고 365일 뒤 삭제된다.

## Play Console 제출 URL

- 개인정보처리방침: `https://public-job-hub.duckdns.org/privacy`
- 계정 삭제: `https://public-job-hub.duckdns.org/account-deletion`
- 이용약관: `https://public-job-hub.duckdns.org/terms`
- 데이터 출처·이용 안내: `https://public-job-hub.duckdns.org/data-sources`

## 출시 직전 수동 검증

1. 테스트 회원에 모든 종류의 개인정보와 게시물을 저장한다.
2. 푸시 연결 후 알림을 받고 연결을 해제해 서버 토큰이 삭제되는지 확인한다.
3. 탈퇴 후 위 테이블에서 테스트 회원 ID·이메일을 검색해 0건인지 확인한다.
4. 비로그인 모바일 브라우저에서 네 개 공개 URL이 열리는지 확인한다.
5. 개인정보처리방침의 운영자·문의 이메일이 Play Console 정보와 일치하는지 확인한다.
6. Play 데이터 보안 설문의 수집·공유·삭제 답변을 실제 출시 AAB의 SDK와 대조한다.
7. 새 SDK, 분석, 광고, 결제 기능을 추가하면 정책·설문·동의 화면을 함께 갱신한다.
