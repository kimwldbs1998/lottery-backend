# CenLottery 파워픽 - Backend (Spring Boot)

`../backend` (JDK 표준 라이브러리만으로 만든 순수 Java 버전)를 Spring Boot로 이식한 버전입니다.
API 경로/요청·응답 JSON 형태는 기존 버전과 동일하게 맞춰서, 프론트엔드(`../frontend`)는 수정 없이 그대로 붙습니다.

원본 `backend` 프로젝트는 건드리지 않았습니다. 두 버전을 비교해보고 마음에 드는 쪽을 쓰시면 됩니다.

## 기술 스택

- Java 21, Spring Boot 3.3
- 빌드: Gradle (Wrapper 포함, 별도 설치 불필요)
- 웹: Spring MVC (내장 Tomcat)
- 인증: Spring Security + JWT (jjwt), 비밀번호는 BCrypt
- 데이터: Spring Data JPA + PostgreSQL (로컬 개발은 H2 인메모리로 대체 가능)

## 빠른 실행 (로컬, DB 설치 불필요)

기본 프로필이 `local`로 설정되어 있어서, 별도 DB 설치 없이 바로 실행됩니다.
데이터는 H2 인메모리 DB에 저장되고 (`MODE=PostgreSQL`로 문법 호환), 프로세스를 재시작하면 초기화됩니다.

```bash
./gradlew bootRun        # macOS/Linux
.\gradlew.bat bootRun     # Windows
```

`http://localhost:7070`에서 뜹니다. 콘솔에 관리자 키(Admin key)가 출력됩니다.
H2 콘솔은 `http://localhost:7070/h2-console`에서 확인 가능합니다 (JDBC URL: `jdbc:h2:mem:cenlottery`, user: `sa`, password 없음).

## "메모리에 올린다"는 게 무슨 뜻인가 (H2 인메모리 DB 상세 설명)

### 무슨 일이 일어나는가

원래 이 프로젝트는 PostgreSQL(디스크에 파일로 저장되는, 별도로 설치/실행해야 하는 DB 서버)을 쓰도록 만들어졌습니다.
로컬 개발 중에는 PostgreSQL을 설치하지 않아도 되게, **H2**라는 자바로 만들어진 가벼운 DB를 대신 씁니다. 그중에서도 "인메모리(in-memory) 모드"로 띄우는데, 이건 디스크에 파일을 전혀 만들지 않고 **애플리케이션 프로세스의 RAM(메모리) 안에만** 테이블/데이터를 만든다는 뜻입니다.

- 서버(Spring Boot 프로세스)가 시작될 때 메모리 위에 빈 DB가 새로 생기고, `ddl-auto: update` 설정 덕분에 Hibernate가 엔티티(`User`, `Round`, `Ticket`, `Purchase` ...) 클래스를 보고 테이블을 자동으로 만들어 줍니다.
- 회원가입/구매 등으로 쌓인 데이터는 전부 이 메모리 위에만 존재합니다.
- **서버 프로세스를 끄면(Ctrl+C, IDE 정지 버튼, 재빌드 후 재시작 등) 그 순간 메모리가 통째로 사라지므로 데이터도 전부 사라집니다.** 다음에 다시 실행하면 완전히 빈 DB로 새로 시작합니다. (디스크 파일이 아예 없기 때문에, 껐다 켜도 남아있는 게 없습니다.)
- 반대로 서버가 켜져 있는 동안에는 평범한 관계형 DB처럼 정상 동작합니다 - 트랜잭션, 조회, 제약조건 다 됩니다. "가짜 DB"가 아니라 "저장 위치가 디스크가 아니라 메모리인 진짜 DB"입니다.

### 실제 설정이 어디 있는가

- `src/main/resources/application.yml` - 공통 설정. `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}` 로 되어 있어서, 환경변수를 따로 안 주면 **기본값이 `local`**입니다.
- `src/main/resources/application-local.yml` - `local` 프로필일 때만 덮어쓰는 설정. 여기서 datasource url을 PostgreSQL 대신 H2로 바꿉니다:
  ```yaml
  spring:
    datasource:
      url: jdbc:h2:mem:cenlottery;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
  ```
  - `jdbc:h2:mem:cenlottery` → `mem:` 이 "디스크 파일이 아니라 메모리에 만들어라"는 뜻입니다. (디스크에 저장하고 싶으면 `jdbc:h2:file:...` 로 바꾸면 되는데, 이 프로젝트에서는 안 씁니다.)
  - `MODE=PostgreSQL` → SQL 문법을 최대한 PostgreSQL과 비슷하게 맞춰서, 나중에 진짜 PostgreSQL로 옮겨도 쿼리가 최대한 그대로 동작하게 해줍니다.
  - `DB_CLOSE_DELAY=-1` → HikariCP 커넥션 풀이 커넥션을 잠깐씩 반납/재획득해도 그 사이에 메모리 DB가 통째로 날아가 버리지 않게, "마지막 연결이 끊겨도 DB를 지우지 말고 프로세스가 살아있는 동안 유지해라"는 옵션입니다. 이게 없으면 요청 중간에 DB가 사라지는 이상한 버그가 날 수 있습니다.
- `build.gradle`에 `runtimeOnly 'com.h2database:h2'` 가 추가되어 있어서, PostgreSQL 드라이버(`org.postgresql:postgresql`)와 H2 드라이버가 둘 다 클래스패스에 있고, 어느 프로필로 뜨느냐에 따라 둘 중 하나가 실제로 쓰입니다.

### 언제 이걸 쓰고, 언제 진짜 PostgreSQL을 써야 하나

- **개발 중 화면/로직 확인, 임시 테스트**: `local`(H2) 그대로 쓰면 됩니다. 설치할 것도 없고, 매번 깨끗한 상태로 시작하니 오히려 편합니다.
- **재시작해도 데이터가 남아있어야 하는 경우, 실제 운영/배포**: 아래 "실제 PostgreSQL로 실행하기"를 따라 `default` 프로필 + 진짜 PostgreSQL로 띄워야 합니다.

## 실제 PostgreSQL로 실행하기

### 1. PostgreSQL 준비

Docker가 있다면:
```bash
docker compose up -d
```
(DB `cenlottery`, 계정 `cenlottery`/`cenlottery`, 포트 5432로 뜹니다)

직접 설치한 PostgreSQL을 쓴다면 `cenlottery` 데이터베이스를 만들고, 아래 환경변수로 접속 정보를 맞춰주세요.
테이블은 Hibernate `ddl-auto: update` 설정으로 첫 실행 시 자동 생성됩니다.

### 2. 애플리케이션 실행

`local` 프로필이 아닌 기본(postgres) 설정으로 띄우려면 `SPRING_PROFILES_ACTIVE`를 비우거나 `default`로 지정하세요.

```bash
SPRING_PROFILES_ACTIVE=default ./gradlew bootRun
```

Windows PowerShell:
```powershell
$env:SPRING_PROFILES_ACTIVE = "default"
.\gradlew.bat bootRun
```

### 환경 변수 (선택)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `PORT` | `7070` | HTTP 포트 |
| `SPRING_PROFILES_ACTIVE` | `local` | `local`(H2, DB 설치 불필요) 또는 `default`(PostgreSQL 연결) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `cenlottery` / `cenlottery` / `cenlottery` | PostgreSQL 접속 정보 (`default` 프로필일 때만 사용) |
| `ADMIN_KEY` | 실행마다 랜덤 생성 | `/api/admin/*` 테스트 API에 필요한 키 |
| `JWT_SECRET` | 실행마다 랜덤 생성 | JWT 서명 키. 여러 인스턴스를 띄우거나 재시작 후에도 로그인 세션을 유지하려면 고정값을 지정하세요 |

## Security / JWT 설정

인증 관련 코드는 `src/main/java/com/cenlottery/` 아래 두 군데로 나뉘어 있습니다 (자바 프로젝트의 표준 경로 규칙대로, 패키지 이름 `com.cenlottery.xxx`가 그대로 `com/cenlottery/xxx` 폴더 경로가 됩니다).

- `config/SecurityConfig.java` - Spring Security 전체 규칙
- `security/` 폴더 - JWT 발급/검증 로직

### `config/SecurityConfig.java`

- `csrf disable` + `sessionCreationPolicy: STATELESS` - 세션/쿠키를 안 쓰고, 매 요청마다 JWT로만 인증하는 API 서버 방식입니다.
- `authorizeHttpRequests`에서 **로그인 없이 접근 가능한** 경로를 지정합니다: `/api/health`, `/api/auth/register`, `/api/auth/login`, `/api/rounds/**`, `/api/admin/**`(대신 `X-Admin-Key` 헤더로 별도 검사). 그 외 나머지 경로(`/api/purchase/**` 등)는 전부 `anyRequest().authenticated()`로 막혀 있어서 로그인(JWT) 없이는 401이 납니다.
- 인증 실패 시(401) 응답을 JSON(`{"error":"SESSION_EXPIRED", ...}`)으로 직접 만들어서 내려주도록 `exceptionHandling`에 커스텀 핸들러를 넣어놨습니다. 프론트가 로그인 여부에 따라 다른 화면을 보여줄 수 있게 하기 위함입니다.
- CORS는 모든 origin(`*`)을 허용하도록 열려 있습니다 - 개발 편의를 위한 설정이니, 실제 배포 시에는 프론트 도메인만 허용하도록 좁혀야 합니다.
- `addFilterBefore(jwtFilter, ...)` - 아래 `JwtAuthenticationFilter`를 Spring Security 필터 체인 맨 앞쪽에 끼워 넣어서, 다른 인증 로직보다 먼저 JWT를 읽게 합니다.

### `security/JwtService.java`

- `jjwt` 라이브러리로 HMAC-SHA256 서명 방식의 JWT를 발급(`issue`)/검증(`verify`)합니다.
- 서명 키는 환경변수 `JWT_SECRET`(→ `app.jwt.secret`)로 지정합니다. **지정하지 않으면 프로세스가 시작될 때마다 랜덤 키를 새로 만듭니다** - 즉, 서버를 재시작하면 그 전에 발급된 토큰(로그인 상태)이 전부 무효화되고 다시 로그인해야 합니다. 로컬 개발 중 "분명 로그인했는데 자꾸 로그아웃된다" 싶으면 대부분 이게 원인입니다 (서버를 재시작했기 때문).
- 토큰 만료 시간은 `app.jwt.expiry-ms` (기본 12시간 = `43200000`ms).

### `security/JwtAuthenticationFilter.java`

- 매 요청마다 `Authorization: Bearer <토큰>` 헤더를 확인합니다.
- 토큰이 있고 유효하면 Spring Security의 `SecurityContext`에 로그인 사용자로 등록해줍니다. 헤더가 없거나 토큰이 유효하지 않아도 이 필터 자체는 요청을 막지 않고 그냥 통과시킵니다 - 실제로 막는 건 위 `SecurityConfig`의 `authorizeHttpRequests` 규칙입니다.

### 관리자(Admin) 인증은 별도

`/api/admin/**`은 JWT가 아니라 `config/AdminKeyHolder.java`가 들고 있는 별도의 관리자 키(`X-Admin-Key` 헤더)로 보호됩니다. 이 키도 `ADMIN_KEY` 환경변수를 안 주면 서버 시작 시마다 랜덤 생성되고, 콘솔에 출력됩니다.

### 관련 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `JWT_SECRET` | 실행마다 랜덤 생성 | 고정하지 않으면 서버 재시작할 때마다 기존 로그인 세션이 전부 무효화됩니다. 여러 인스턴스를 띄우거나 재시작 후에도 세션을 유지하려면 반드시 고정값을 지정하세요 |
| `ADMIN_KEY` | 실행마다 랜덤 생성 | `/api/admin/*` 테스트 API 호출 시 `X-Admin-Key` 헤더에 넣어야 하는 값. 서버 콘솔 출력에서 확인 가능 |

## 원본(순수 Java) 버전과의 차이

- HTTP 서버(`com.sun.net.httpserver`) → Spring MVC
- 직접 만든 JWT/JSON/라우터 → Spring Security + jjwt + Jackson(Spring 내장)
- 메모리 저장소 + `data/db.json` 파일 스냅샷 → PostgreSQL + Spring Data JPA (로컬 개발은 H2로 대체 가능)
- 비밀번호 해시: PBKDF2 직접 구현 → `BCryptPasswordEncoder`

API 경로, 요청/응답 JSON의 필드명과 타입(금액은 원본과 동일하게 문자열로 내려줍니다)은 그대로 유지했습니다.

## 알아두면 좋은 점

- `src/com/cenlottery/test/AllTests.java`(23개 어서션 테스트)는 아직 JUnit으로 이식하지 않았습니다. 필요하시면 요청해 주세요.
- 회차 정산(`RoundService.settleAndAdvance`)은 티켓 저장 → 회차 저장 → 다음 회차 오픈, 세 단계를 한 트랜잭션으로 묶으려 했으나 같은 클래스 내부 호출(self-invocation)이라 Spring의 `@Transactional` 프록시가 적용되지 않는 한계가 있습니다. 각 저장 호출 자체는 개별적으로 커밋되므로 기능은 정상 동작하지만, 완전한 원자성이 필요하면 이 부분을 별도 빈으로 분리해야 합니다.
- 프로덕션에서는 `ddl-auto: update` 대신 Flyway/Liquibase 마이그레이션을 쓰는 걸 권장합니다.
- `local` 프로필(H2)은 로컬 개발/테스트용입니다. 데이터가 영구 저장되지 않으므로 실제 운영에는 `default` 프로필 + PostgreSQL을 사용하세요.
- 단일 서버 프로세스 기준입니다 (원본과 동일한 가정).
