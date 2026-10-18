# Public Job Hub 운영 매뉴얼

최종 갱신: 2026-10-18  
운영 주소: `https://public-job-hub.duckdns.org`  
운영 환경: OCI Ubuntu 22.04 Minimal, Docker Compose, Caddy, TiDB Cloud Starter

이 문서는 Public Job Hub의 배포, 점검, 백업, 복구와 장애 대응 절차를 설명한다. 명령에 비밀번호, TiDB 접속정보, 공공데이터 인증키를 직접 적거나 Git에 커밋하지 않는다.

## 1. 운영 구성

| 구성요소 | 역할 |
|---|---|
| OCI `VM.Standard.E2.1.Micro` | Spring Boot와 Caddy 실행 |
| Docker Compose | 앱과 Caddy 생명주기 관리 |
| Caddy | HTTPS 인증서 발급과 리버스 프록시 |
| TiDB Cloud Starter | 운영 MySQL 호환 데이터베이스 |
| DuckDNS | `public-job-hub.duckdns.org` 도메인 |
| GitHub `main` | 운영 배포 기준 소스 |

OCI 프로젝트 위치:

```bash
~/job-portal/public-job-hub
```

중요 파일:

| 파일 | 용도 |
|---|---|
| `.env` | 운영 비밀값과 접속 설정. Git에 포함하지 않음 |
| `compose-tidb.yml` | 앱·Caddy 운영 구성 |
| `Caddyfile` | HTTPS와 프록시 설정 |
| `backups/tidb/` | 최근 7일 TiDB 논리 백업 |
| `backups/production-health.log` | 운영 상태 자동 점검 로그 |
| `backups/tidb-backup.log` | DB 백업 로그 |
| `backups/storage-maintenance.log` | 서버 용량 관리 로그 |

## 2. 보안 원칙

- `.env`, TiDB 비밀번호, 관리자 비밀번호, API 인증키를 채팅·문서·Git에 올리지 않는다.
- SSH 개인키는 본인 Windows 계정만 읽을 수 있게 유지한다.
- OCI 방화벽은 외부에 `22`, `80`, `443` 포트만 연다.
- SSH 비밀번호 로그인과 root 로그인을 활성화하지 않는다.
- Docker 볼륨 정리 명령인 `docker volume prune`은 실행하지 않는다.
- 운영 DB를 직접 삭제하거나 기존 백업으로 덮어쓰지 않는다.
- 복구는 항상 임시 DB에서 먼저 검증한 뒤 별도의 점검을 거쳐 진행한다.

## 3. 서버 접속과 종료

Windows PowerShell에서 접속한다.

```powershell
ssh -i "$env:USERPROFILE\.ssh\public-job-hub-oci.key" ubuntu@161.33.8.82
```

OCI 공인 IP가 변경되면 Oracle Cloud의 인스턴스 화면에서 새 IP를 확인해 명령을 수정한다. 서버 프롬프트가 다음처럼 바뀌면 접속된 상태다.

```text
ubuntu@jobhub-vcn:~$
```

연결만 종료할 때는 서버를 종료하지 말고 다음을 실행한다.

```bash
exit
```

## 4. 정기 자동 작업

현재 운영 일정은 한국시간 기준이다.

| 일정 | 작업 |
|---|---|
| 매시간 7분 | 앱·HTTPS·TiDB·메모리·디스크·백업 상태 점검 |
| 매일 03:30 | TiDB 논리 백업 생성, 최근 7일 보관 |
| 매주 일요일 04:30 | 7일 이상 된 미사용 Docker 캐시와 오래된 로그 정리 |
| 매주 일요일 05:00 | 최신 TiDB 백업을 암호화해 OCI Object Storage에 업로드 |

등록 내용을 확인한다.

```bash
crontab -l
systemctl is-active cron
```

`cron` 결과가 `active`여야 한다. 자동 작업 설치 스크립트는 다시 실행해도 같은 작업을 중복 등록하지 않도록 구성되어 있다.

## 5. 코드 업데이트와 운영 배포

### 5.1 Windows에서 커밋

```powershell
cd "C:\Users\kwi07\OneDrive\Desktop\project\job-portal\public-job-hub"
git status
git add <변경한 파일>
git commit -m "변경 내용을 설명하는 메시지"
git push origin main
```

`git add .`보다 변경한 파일을 명시하는 방식을 권장한다. `.env`나 백업 파일이 포함되지 않았는지 `git status`로 확인한다.

### 5.2 OCI에서 배포

```bash
cd ~/job-portal
git pull origin main

cd public-job-hub
docker compose -f compose-tidb.yml --env-file .env up -d --build app
docker compose -f compose-tidb.yml --env-file .env ps
```

OCI 무료 서버에서는 빌드가 수 분 걸릴 수 있다. 빌드 중 SSH 창을 닫거나 `Ctrl+C`를 누르지 않는다. `app`이 잠시 `health: starting`이면 1~2분 뒤 다시 확인한다.

```bash
docker compose -f compose-tidb.yml --env-file .env ps
```

최종적으로 앱이 `healthy`, Caddy가 `Up`이어야 한다. 브라우저에는 이전 PWA 파일이 남을 수 있으므로 화면이 갱신되지 않으면 `Ctrl+F5`로 새로고침한다.

### 5.3 설정만 변경한 경우

Compose 설정이나 `.env`를 변경했다면 해당 컨테이너를 재생성한다.

```bash
docker compose -f compose-tidb.yml --env-file .env up -d app caddy
```

## 6. 일상 상태 점검

전체 운영 상태를 한 번에 확인한다.

```bash
cd ~/job-portal/public-job-hub
./scripts/check-production-health.sh
```

정상 결과의 마지막 줄은 다음과 같다.

```text
RESULT: HEALTHY
```

자동 점검 로그:

```bash
tail -n 80 backups/production-health.log
```

컨테이너 상태와 자원 상태를 따로 확인할 수 있다.

```bash
docker compose -f compose-tidb.yml --env-file .env ps
free -h
df -h
docker system df
```

판단 기준:

- 가용 메모리 128MB 미만: 주의
- 디스크 사용률 85% 이상: 주의
- 최신 DB 백업 30시간 초과: 백업 실패로 판단
- 앱 상태 `unhealthy` 또는 HTTPS 실패: 즉시 로그 확인
- 최근 채용공고 수집 실패: 관리자 화면에서 실패 출처와 원인 확인

### 6.1 무료 Discord 장애 알림

상태 점검, TiDB 백업, 저장공간 정리가 실패하면 Discord로 한 번 알리고,
다음 실행에서 정상으로 돌아왔을 때 복구 알림을 한 번 보낸다. 같은 장애가 계속되는
동안에는 반복 알림을 보내지 않는다.

1. Discord에서 운영 알림을 받을 비공개 서버와 채널을 준비한다.
2. 채널의 `채널 편집 → 연동 → 웹후크 → 새 웹후크`를 선택한다.
3. 이름과 알림 채널을 확인하고 `웹후크 URL 복사`를 누른다.
4. OCI 서버의 `.env`에 다음 값을 추가한다. 따옴표는 사용하지 않는다.

```ini
JOBHUB_DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/발급값
```

설정 후 테스트 알림을 보낸다.

```bash
cd ~/job-portal/public-job-hub
chmod 700 scripts/send-ops-alert.sh scripts/monitor-command.sh
./scripts/send-ops-alert.sh TEST "Public Job Hub 알림 테스트" "운영 알림 연결이 완료되었습니다."
```

Discord 채널에서 메시지를 받은 뒤 세 자동 작업을 다시 설치한다.

```bash
./scripts/install-health-check-cron.sh
./scripts/install-tidb-backup-cron.sh
./scripts/install-storage-maintenance-cron.sh
crontab -l
```

웹훅 URL은 메시지를 보낼 수 있는 비밀번호와 같으므로 Git에 커밋하거나 화면 캡처로
공유하지 않는다. Discord 알림을 사용하지 않을 때는 값을 비워 두면 기존 자동 작업만
실행되고 알림은 건너뛴다.

## 7. 로그 확인

앱 최근 로그:

```bash
docker compose -f compose-tidb.yml --env-file .env logs --tail=150 app
```

Caddy 최근 로그:

```bash
docker compose -f compose-tidb.yml --env-file .env logs --tail=150 caddy
```

로그를 실시간으로 볼 때는 `Ctrl+C`로 조회만 종료한다. 컨테이너는 종료되지 않는다.

```bash
docker compose -f compose-tidb.yml --env-file .env logs -f app
```

Docker 로그는 앱과 Caddy 각각 `10MB × 3개`로 제한되어 있다.

## 8. 채용공고 수집 운영

1. 관리자 계정으로 로그인한다.
2. 관리자 화면에서 수동 수집을 실행한다.
3. 처리 건수와 실패 내용을 확인한다.
4. 공개 화면에서 기관·마감일·지역·직렬·원문 링크를 표본 점검한다.

수집 오류에 SQL 문구가 있으면 앱 로그에서 원인을 확인한다.

```bash
docker compose -f compose-tidb.yml --env-file .env logs --tail=200 app
```

대표적인 대응:

| 증상 | 대응 |
|---|---|
| `Data too long` | 최신 코드가 배포됐는지 확인하고 앱 재빌드 후 다시 수집 |
| API `403`, 서비스키 오류 | `.env`의 공공데이터 인증키 확인. 로그나 화면에 키를 출력하지 않음 |
| 외부 원문 접속 실패 | 해당 사이트의 장애·robots.txt·URL 변경 여부 확인 |
| 수집 처리 0건 | API 응답과 필터 범위, 관리자 수집 결과 상세 확인 |
| 동일 수집이 실행 중 | 기존 수집이 종료될 때까지 기다린 후 다시 실행 |

오류가 반복되어도 DB를 직접 수정하지 말고 로그와 원문을 먼저 확인한다.

## 9. 백업 확인

백업 목록:

```bash
ls -lh backups/tidb
tail -n 80 backups/tidb-backup.log
```

수동 백업:

```bash
./scripts/backup-tidb.sh
```

백업 성공 문구와 새 `.sql.gz` 파일을 확인한다. 백업 파일 권한은 `600`이어야 한다.

```bash
ls -l backups/tidb
```

### 9.1 OCI Object Storage 외부 암호화 백업

서버 디스크 장애에 대비해 매주 최신 백업을 AES-256으로 암호화한 후 비공개
Object Storage 버킷에 업로드한다. 암호화되지 않은 SQL 파일은 외부로 전송하지 않는다.

OCI 콘솔에서 다음 순서로 준비한다.

1. `Storage → Object Storage & Archive Storage → Buckets`로 이동한다.
2. `Create bucket`을 눌러 `public-job-hub-backups` 비공개 버킷을 만든다.
3. 버킷의 `Lifecycle Policy Rules`에서 56일이 지난 객체를 삭제하는 규칙을 만든다.
4. 버킷의 `Pre-Authenticated Requests`에서 새 요청을 만든다.
5. 대상은 `Bucket`, 권한은 `Permit object writes`로 설정하고 객체 목록 허용은 끈다.
6. 만료일은 1년 뒤로 정하고, 생성 직후 한 번만 표시되는 URL을 복사한다.
7. 웹훅과 마찬가지로 이 URL을 채팅, Git, 화면 캡처에 노출하지 않는다.

서버 `.env`에는 다음 두 값을 저장한다.

```ini
JOBHUB_BACKUP_OBJECT_STORAGE_URL=https://objectstorage.<region>.oraclecloud.com/p/.../o/
JOBHUB_BACKUP_ENCRYPTION_PASSWORD=24자_이상의_별도_강력한_비밀번호
```

암호화 비밀번호를 잃으면 외부 백업을 복구할 수 없다. 반드시 서버 밖의 비밀번호
관리자에 별도로 저장하며 Git이나 일반 메모장에는 기록하지 않는다.

수동 업로드와 자동 작업 등록:

```bash
chmod 700 scripts/upload-backup-object-storage.sh scripts/install-offsite-backup-cron.sh
./scripts/upload-backup-object-storage.sh
./scripts/install-offsite-backup-cron.sh
crontab -l
```

정상 결과에는 `Encrypted off-site backup uploaded`가 표시되고, OCI 버킷의 Objects
목록에는 `.aes256` 파일이 생성된다. 실패와 이후 복구는 Discord로 통지된다.

복구할 때는 OCI 콘솔에서 암호화 파일을 다운로드한 뒤 다음과 같이 복호화한다.

```bash
read -r -s -p "Backup encryption password: " BACKUP_PASSWORD
echo
export BACKUP_PASSWORD
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
  -in public-job-hub-tidb-날짜.sql.gz.aes256 \
  -out public-job-hub-tidb-restored.sql.gz \
  -pass env:BACKUP_PASSWORD
unset BACKUP_PASSWORD
gzip -t public-job-hub-tidb-restored.sql.gz
```

PAR URL이 만료되기 전에 새 쓰기 전용 요청을 만들고 `.env`의 URL을 교체한다.

## 10. 백업 복구 검증

운영 DB를 변경하지 않고 최신 백업을 임시 DB에 복원한다.

```bash
./scripts/verify-tidb-backup.sh
```

검증 과정:

1. 최신 gzip 파일 무결성 확인
2. `public_job_hub_restore_test` 임시 DB 생성
3. 백업 복원
4. 운영 DB와 테이블 개수 비교
5. 임시 DB 자동 삭제

정상 결과에는 `TiDB restore verification completed successfully`가 표시된다. 실패한 백업으로 운영 DB를 복구하지 않는다.

### 실제 재해 복구 원칙

실제 운영 복구가 필요하면 다음 원칙을 지킨다.

1. 앱 쓰기 작업과 관리자 수집을 중단한다.
2. 기존 운영 DB를 삭제하지 않는다.
3. 새로운 이름의 복구 DB를 생성한다.
4. 백업을 새 DB에 복원한다.
5. 테이블 수와 핵심 회원·공고 데이터 건수를 확인한다.
6. 새로운 `MYSQL_URL`로 앱을 전환한 후 점검한다.
7. 문제가 없을 때만 기존 DB 보존 정책을 결정한다.

운영 DB 삭제나 접속정보 전환은 되돌리기 어려우므로 실행 전에 별도 검토한다.

## 11. 서버 용량 관리

수동 점검 및 안전 정리:

```bash
./scripts/maintain-server-storage.sh
```

이 스크립트는 다음 항목만 정리한다.

- 7일 이상 된 중지 컨테이너
- 7일 이상 된 dangling 이미지
- 7일 이상 된 미사용 빌드 캐시
- 5MB를 넘은 운영 로그의 오래된 부분

실행 중인 컨테이너, 사용 중인 이미지, 모든 Docker 볼륨과 DB 백업은 보존한다.

자동 정리 로그:

```bash
tail -n 80 backups/storage-maintenance.log
```

디스크가 급격히 증가하면 큰 디렉터리를 확인한다.

```bash
docker system df
du -h -d 2 ~/job-portal/public-job-hub | sort -h | tail -n 20
```

`docker volume prune`, 프로젝트 디렉터리 전체 삭제, `.env` 삭제는 금지한다.

## 12. 장애 대응

### 12.1 사이트가 열리지 않음

```bash
cd ~/job-portal/public-job-hub
docker compose -f compose-tidb.yml --env-file .env ps
curl -I https://public-job-hub.duckdns.org
```

- 앱이 `unhealthy`: 앱 로그 확인
- Caddy가 내려감: Caddy 로그 확인 후 `docker compose ... up -d caddy`
- 도메인이 다른 IP를 가리킴: DuckDNS IP를 OCI 공인 IP로 갱신
- OCI 보안 규칙에서 80·443 포트가 열려 있는지 확인

### 12.2 앱이 unhealthy

```bash
docker compose -f compose-tidb.yml --env-file .env logs --tail=200 app
free -h
```

설정 오류를 수정한 뒤 앱만 다시 생성한다.

```bash
docker compose -f compose-tidb.yml --env-file .env up -d app
```

원인을 확인하지 않은 채 반복 재시작하지 않는다.

### 12.3 TiDB 연결 실패

- TiDB Cloud 인스턴스 상태와 Connect 정보를 확인한다.
- `.env`의 `MYSQL_URL`, 사용자명, 비밀번호를 서버 안에서만 확인한다.
- 호스트 자리표시자 `<tidb-host>`나 `<database>`가 남아 있지 않은지 확인한다.
- 비밀번호를 터미널 출력이나 Git에 남기지 않는다.
- 점검 후 앱을 재생성하고 상태 스크립트를 다시 실행한다.

```bash
docker compose -f compose-tidb.yml --env-file .env up -d app
./scripts/check-production-health.sh
```

### 12.4 메모리 부족

```bash
free -h
sudo swapon --show
docker stats --no-stream
```

- 2GB swap이 표시되는지 확인한다.
- 동시에 여러 Docker 빌드를 실행하지 않는다.
- 빌드 중에는 다른 무거운 작업을 피한다.
- 앱 메모리 제한 640MB와 Caddy 128MB를 임의로 크게 늘리지 않는다.

### 12.5 디스크 부족

```bash
df -h
docker system df
./scripts/maintain-server-storage.sh
```

자동 정리 후에도 85% 이상이면 원인을 먼저 확인한다. 볼륨과 백업을 임의 삭제하지 않는다.

### 12.6 SSH 접속 실패

Windows에서 다음을 확인한다.

```powershell
Test-NetConnection 161.33.8.82 -Port 22
ssh -v -i "$env:USERPROFILE\.ssh\public-job-hub-oci.key" ubuntu@161.33.8.82
```

- OCI 인스턴스가 Running인지 확인
- 공인 IP 변경 여부 확인
- OCI 인바운드 TCP 22 규칙 확인
- 개인키 경로와 Windows 파일 권한 확인
- 짧은 시간 반복 실패했다면 Fail2ban 차단이 풀릴 때까지 기다림

## 13. 서버 재부팅 후 점검

OCI 서버가 재부팅되면 Docker 컨테이너는 `restart: unless-stopped` 정책으로 자동 시작한다. 2~3분 뒤 접속하여 확인한다.

```bash
cd ~/job-portal/public-job-hub
docker compose -f compose-tidb.yml --env-file .env ps
free -h
sudo swapon --show
systemctl is-active docker
systemctl is-active cron
systemctl is-active fail2ban
./scripts/check-production-health.sh
```

모든 서비스가 정상이고 결과가 `HEALTHY`여야 한다.

## 14. 계정과 비밀값 관리

- 관리자 비밀번호는 16자 이상의 고유 비밀번호를 사용한다.
- 공공데이터 API 키는 `.env`에서만 관리한다.
- TiDB 비밀번호를 변경하면 OCI `.env`도 갱신한 뒤 앱을 재생성한다.
- `.env` 권한을 유지한다.

```bash
chmod 600 .env
ls -l .env
```

- SSH 개인키를 분실하면 기존 키를 공개 저장소에 올리지 말고 OCI에서 새 키 등록 절차를 진행한다.

## 15. 운영 체크리스트

### 매일 또는 장애 신고 시

- 사이트 HTTPS 접속
- `production-health.log`의 최근 결과가 `HEALTHY`인지 확인
- 채용공고 마지막 수집 시각과 처리 건수 확인
- `tidb-backup.log`에서 당일 백업 성공 확인

### 매주

- 수집된 기관과 직렬을 표본 검수
- 관리자 오류 신고와 Q&A 확인
- 디스크와 Docker 사용량 확인
- `storage-maintenance.log` 확인

### 매월

- `verify-tidb-backup.sh`로 실제 복구 검증
- 관리자·TiDB·GitHub 계정 접근 권한 확인
- 운영 문서와 서버 IP·도메인 정보 최신화
- 오래된 사용자 문의와 공고 변경 이력 보존 정책 확인

## 16. 긴급 점검 명령 모음

```bash
cd ~/job-portal/public-job-hub

docker compose -f compose-tidb.yml --env-file .env ps
docker compose -f compose-tidb.yml --env-file .env logs --tail=150 app
docker compose -f compose-tidb.yml --env-file .env logs --tail=150 caddy
./scripts/check-production-health.sh
free -h
df -h
docker system df
crontab -l
```

장애 대응 중에는 실행한 명령, 시각, 오류 메시지와 해결 결과를 기록한다. 비밀번호와 인증키는 기록에서 제외한다.
