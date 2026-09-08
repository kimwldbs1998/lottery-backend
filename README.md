# CenLottery 파워픽 - Backend (Spring Boot)

`../backend` (JDK 표준 라이브러리만으로 만든 순수 Java 버전)를 Spring Boot로 이식한 버전입니다.
API 경로/요청·응답 JSON 형태는 기존 버전과 동일하게 맞춰서, 프론트엔드(`../frontend`)는 수정 없이 그대로 붙습니다.

원본 `backend` 프로젝트는 건드리지 않았습니다. 두 버전을 비교해보고 마음에 드는 쪽을 쓰시면 됩니다.

## 기술 스택

- Java 21, Spring Boot 3.3
- 빌드: Gradle
- 웹: Spring Web (MVC)
- 인증: Spring Security + JWT (jjwt), 비밀번호는 BCrypt
- 데이터: Spring Data JPA + PostgreSQL

## 실행 방법

### 1. PostgreSQL 준비

Docker가 있다면:
```bash
docker compose up -d
```
(DB `cenlottery`, 계정 `cenlottery`/`cenlottery`, 포트 5432로 뜹니다)

직접 설치한 PostgreSQL을 쓴다면 `cenlottery` 데이터베이스를 만들고, 아래 환경변수로 접속 정보를 맞춰주세요.
테이블은 Hibernate `ddl-auto: update` 설정으로 첫 실행 시 자동 생성됩니다.

### 2. 애플리케이션 실행

이 저장소에는 Gradle Wrapper(`gradlew`)가 포함되어 있지 않습니다 (실행 환경에 Gradle/JDK가 없어 생성하지 못했습니다).
로컬에 Gradle 8.x + JDK 21이 설치되어 있다면:
```bash
gradle wrapper   # 최초 1회, gradlew/gradlew.bat 생성
./gradlew bootRun
```
또는 IntelliJ IDEA / VS Code(Java + Gradle 확장)로 이 폴더를 열면 IDE가 Gradle을 자동으로 받아 실행해 줍니다.

기본적으로 `http://localhost:8080`에서 뜹니다. 콘솔에 관리자 키(Admin key)가 출력됩니다.

### 환경 변수 (선택)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `PORT` | `8080` | HTTP 포트 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `cenlottery` / `cenlottery` / `cenlottery` | PostgreSQL 접속 정보 |
| `ADMIN_KEY` | 실행마다 랜덤 생성 | `/api/admin/*` 테스트 API에 필요한 키 |
| `JWT_SECRET` | 실행마다 랜덤 생성 | JWT 서명 키. 여러 인스턴스를 띄우거나 재시작 후에도 로그인 세션을 유지하려면 고정값을 지정하세요 |

## 원본(순수 Java) 버전과의 차이

- HTTP 서버(`com.sun.net.httpserver`) → Spring MVC
- 직접 만든 JWT/JSON/라우터 → Spring Security + jjwt + Jackson(Spring 내장)
- 메모리 저장소 + `data/db.json` 파일 스냅샷 → PostgreSQL + Spring Data JPA
- 비밀번호 해시: PBKDF2 직접 구현 → `BCryptPasswordEncoder`

API 경로, 요청/응답 JSON의 필드명과 타입(금액은 원본과 동일하게 문자열로 내려줍니다)은 그대로 유지했습니다.

## 알아두면 좋은 점

- `src/com/cenlottery/test/AllTests.java`(23개 어서션 테스트)는 아직 JUnit으로 이식하지 않았습니다. 필요하시면 요청해 주세요.
- 회차 정산(`RoundService.settleAndAdvance`)은 티켓 저장 → 회차 저장 → 다음 회차 오픈, 세 단계를 한 트랜잭션으로 묶으려 했으나 같은 클래스 내부 호출(self-invocation)이라 Spring의 `@Transactional` 프록시가 적용되지 않는 한계가 있습니다. 각 저장 호출 자체는 개별적으로 커밋되므로 기능은 정상 동작하지만, 완전한 원자성이 필요하면 이 부분을 별도 빈으로 분리해야 합니다.
- 프로덕션에서는 `ddl-auto: update` 대신 Flyway/Liquibase 마이그레이션을 쓰는 걸 권장합니다.
- 단일 서버 프로세스 기준입니다 (원본과 동일한 가정).

실행할 때 명령어
.\gradlew.bat bootRun