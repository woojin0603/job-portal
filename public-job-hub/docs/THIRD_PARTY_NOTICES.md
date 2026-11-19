# Public Job Hub 제3자 소프트웨어 고지

기준일: 2026-11-19

Public Job Hub는 아래 주요 오픈소스 소프트웨어를 사용한다. 이 목록은 직접 의존성을 중심으로 작성했으며, 배포 전 실제 빌드의 전이 의존성 라이선스 보고서를 다시 생성해 함께 보관해야 한다. 각 저작권과 라이선스는 해당 권리자에게 있다.

## 서버

| 구성요소 | 용도 | 주요 라이선스 |
| --- | --- | --- |
| Spring Boot 및 Spring Framework | 웹, 보안, 데이터, 메일, 운영 상태 | Apache License 2.0 |
| Firebase Admin Java SDK | 푸시 알림 발송 | Apache License 2.0 |
| jsoup | 외부 HTML 파싱·정제 | MIT License |
| Apache PDFBox | PDF 텍스트·첨부 처리 | Apache License 2.0 |
| Apache POI | 스프레드시트 처리 | Apache License 2.0 |
| MySQL Connector/J | 운영 DB 연결 | GNU GPL v2 with Universal FOSS Exception |
| H2 Database Engine | 개발·테스트 DB | MPL 2.0 또는 EPL 1.0 |

## 웹·설치형 앱

| 구성요소 | 용도 | 주요 라이선스 |
| --- | --- | --- |
| React / React DOM | 사용자 인터페이스 | MIT License |
| Firebase Web SDK | 웹 푸시 연결 | Apache License 2.0 |
| Vite | 프런트엔드 빌드 | MIT License |
| vite-plugin-pwa | PWA와 서비스 워커 빌드 | MIT License |
| Prettier | 개발 중 코드 포맷 | MIT License |

## 배포 전 검증

- `frontend/package-lock.json`과 Gradle의 실제 해석 결과를 기준으로 버전·라이선스를 재확인한다.
- Apache 2.0 등 NOTICE 보존 의무가 있는 구성요소의 배포 파일을 확인한다.
- 라이선스가 불명확하거나 상업적 배포 조건과 충돌하는 새 의존성을 추가하지 않는다.
- 이 문서는 라이선스 원문을 대체하지 않는다. 배포물에는 각 라이선스가 요구하는 원문과 저작권 고지를 포함한다.

