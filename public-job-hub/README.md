# Public Job Hub (Java 21 + React)

Spring Boot 프로젝트를 `build.gradle`로 가져와 Java 21로 실행하세요. React는 `frontend` 폴더에서 `npm install` 후 `npm run dev`로 실행합니다. 화면 주소는 http://localhost:5173 이며 Vite가 `/api` 요청을 Spring Boot의 8080 포트로 전달합니다. PowerShell에서 npm 스크립트가 차단되면 `npm.cmd`를 사용하세요.

## 휴대폰 앱처럼 설치하기 (PWA)

`frontend`에서 `npm install`을 한 번 수행한 뒤 프로젝트 루트에서 `./gradlew bootJar`(Windows: `gradlew.bat bootJar`)를 실행하세요. Gradle이 React 화면, 설치 정보, 192·512px 아이콘과 서비스 워커를 빌드해 Spring Boot JAR 안에 넣습니다. 생성된 `build/libs/public-job-hub-0.1.0.jar`을 `java -jar`로 실행하면 앱 화면과 API가 함께 http://localhost:8080 에 제공됩니다.

휴대폰에서는 **HTTPS로 배포된 같은 주소**에 접속해 브라우저의 '홈 화면에 추가' 또는 '앱 설치'를 선택하세요. 설치를 지원하는 브라우저에서는 화면 상단에도 '앱 설치' 버튼이 나타납니다. 개발용 localhost는 설치 테스트에 쓸 수 있지만, 휴대폰에서 PC의 로컬 주소로 접속하는 HTTP 방식은 설치 조건을 충족하지 못할 수 있습니다. Vite 개발 서버(`npm run dev`)보다 빌드된 JAR에서 설치를 확인하세요.

서비스 워커는 React 앱 화면과 아이콘 같은 정적 파일만 오프라인용으로 캐시합니다. 공고 조회·로그인·스크랩·원문 미리보기 API는 캐시하지 않으므로 인터넷과 Spring Boot 서버가 필요합니다. 새 버전의 정적 파일이 배포되면 서비스 워커가 자동 갱신됩니다.

## 계정과 스크랩

React 화면에서 이메일·이름·비밀번호(8자 이상)로 회원가입한 뒤 로그인할 수 있습니다. 비밀번호는 BCrypt 해시로 H2 DB에 저장됩니다. 인증은 Spring Security 세션을 사용하고, 변경 요청은 CSRF 토큰으로 보호합니다. 공고 목록은 비로그인 상태에서도 볼 수 있지만 스크랩과 마이페이지는 로그인이 필요합니다. 스크랩과 지원 완료 표시는 로그인한 사용자별로 저장됩니다. 이전 데모 사용자는 더 이상 자동 생성하지 않습니다.

## 수집과 원문 미니탭

잡알리오 첫 페이지를 **매일 00:00 Asia/Seoul**에 한 번 수집합니다. 서버가 자정에 실행 중이어야 예약 작업이 수행됩니다. 서버 시작 시나 화면 새로고침 시 추가 수집하지 않습니다. 표 파싱이 실패하고 `OPENAI_API_KEY`가 있으면 OpenAI Responses API로 추출을 시도합니다. `OPENAI_MODEL`로 모델을 변경할 수 있습니다. 수집 상태는 화면과 `/api/crawl-status`에서 확인합니다.

공고 제목 또는 '미니탭에서 보기'를 누르면 오른쪽 패널에 원문 텍스트가 표시됩니다. 서버가 해당 공고의 원문 페이지를 읽어 스크립트·스타일 등은 제거하고 텍스트만 반환합니다. 원문 사이트가 서버 접근을 차단하거나 JavaScript 렌더링에 의존하면 패널에 오류가 표시될 수 있습니다. 미니탭 조회는 예약 수집을 실행하지 않습니다.

## DB 구조

| 테이블 | 주요 필드 | 제약 |
|---|---|---|
| `app_users` | `email`, `password_hash`, `display_name`, `created_at` | 이메일 유일 |
| `job_postings` | `source`, `source_id`, `title`, `organization`, `region`, `employment_type`, `posted_at`, `deadline`, `source_url`, `open`, `first_seen_at`, `updated_at` | 출처·원본 ID 유일 |
| `scraps` | `user_id`, `posting_id`, `applied`, `scrapped_at`, `applied_at` | 사용자·공고 조합 유일 |

H2 파일은 `./data/jobhub`에 저장됩니다. 운영 배포에는 PostgreSQL, Flyway 마이그레이션, HTTPS, 수집 출처별 파서와 실패 알림이 필요합니다. 잡알리오 외 사이트는 각 사이트의 표 구조와 이용 조건을 확인해 어댑터를 추가해야 합니다. 현재는 목록 첫 페이지만 수집합니다.

## 소스 코드 형식과 주석

Java 메서드·레코드와 React 함수·객체, CSS 규칙은 선언 줄에 `{`를 두고 내용과 `}`를 다음 줄에 배치합니다. 도메인 클래스, 저장소 조회, API 기능, 수집 단계, React 화면 구성 바로 위에 역할과 처리 이유를 설명하는 주석을 두었습니다. React/CSS는 `frontend`에서 `npm run format`으로 다시 정리하고 `npm run format:check`로 검사할 수 있습니다. JSON·YAML처럼 주석 문법이 제한되거나 다른 형식이 필요한 파일은 해당 파일 문법을 우선합니다. Gradle Wrapper, `package-lock.json`, 빌드 산출물과 DB 파일은 자동 생성물로 취급합니다.

## 현재 공고 수동 갱신

예약 수집은 서울 시간 자정에만 실행됩니다. 자정 전에 즉시 갱신하려면 실행 중인 Spring Boot 서버를 먼저 종료한 후 프로젝트 폴더에서 다음 명령을 한 번 실행하세요. 기존 H2 데이터베이스를 사용하므로 회원과 스크랩 데이터는 유지되며, 공고는 출처와 원본 ID를 기준으로 갱신됩니다.

```powershell
java -jar build/libs/public-job-hub-0.1.0.jar --server.port=0 --jobhub.crawl.run-once=true
```

콘솔에 `Immediate crawl: count=20, error=null`처럼 나오면 성공입니다. 이후 평소처럼 서버를 다시 실행하고 `/api/postings` 또는 화면을 새로고침하세요. 기본 설정은 잡알리오 최신 목록 2페이지(최대 20건)를 수집하며, `jobhub.crawl.pages` 설정으로 페이지 수를 조정할 수 있습니다. 수동 실행은 매일 자정 예약 주기를 변경하지 않습니다.
