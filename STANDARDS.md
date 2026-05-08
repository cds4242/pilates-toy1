# 공통규칙 (STANDARDS.md)

> 이 문서는 프로젝트 전반에 걸쳐 지켜야 할 기술 표준과 규칙을 정의한다.
> 모든 코드는 이 문서의 기준을 따른다.

---

## 1. 기술 스택

### Backend
- **언어/프레임워크**: Java 21 + Spring Boot 3.x
- **ORM**: Spring Data JPA + QueryDSL 5.x
- **Database**: MySQL 8.0
- **Cache/Session**: Redis 7
- **빌드**: Gradle (Kotlin DSL)
- **API 문서**: SpringDoc OpenAPI (Swagger UI)

### Frontend
- **프레임워크**: Next.js 14 (App Router)
- **언어**: TypeScript (strict mode)
- **스타일링**: TailwindCSS + shadcn/ui
- **상태 관리**: Zustand (클라이언트 상태) + React Query / TanStack Query (서버 상태)
- **패키지 매니저**: pnpm
- **폼 관리**: React Hook Form + Zod (유효성 검증)

### 인프라
- **컨테이너**: Docker + Docker Compose (개발 환경)
- **CI/CD**: GitHub Actions
- **배포**: 추후 결정 (1차는 Docker Compose 기반)

---

## 2. 백엔드 아키텍처

### 패키지 구조 (도메인 주도 설계)

```
com.pilates.studio
├── global/                    # 전역 설정, 공통 모듈
│   ├── config/                # Spring 설정 클래스
│   ├── error/                 # 예외 처리 (BusinessException, ErrorCode)
│   ├── response/              # 공통 응답 포맷 (ApiResponse)
│   ├── security/              # JWT, 인증/인가
│   ├── util/                  # 유틸리티
│   └── logging/               # 로깅 설정, 마스킹
├── domain/
│   ├── member/                # 회원
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   ├── ticket/                # 수강권
│   ├── reservation/           # 예약
│   ├── schedule/              # 스케줄
│   ├── lesson/                # 수업
│   ├── instructor/            # 강사
│   ├── revenue/               # 매출
│   └── notification/          # 알림
└── PilatesStudioApplication.java
```

### 계층 규칙
- **Controller**: HTTP 요청/응답 처리만. 비즈니스 로직 금지.
- **Service**: 비즈니스 로직 담당. 트랜잭션 경계.
- **Repository**: 데이터 접근. JPA + QueryDSL 커스텀 쿼리.
- **Entity**: JPA 엔티티. `@Entity` 클래스에 비즈니스 메서드 포함 가능 (Rich Domain Model).
- **DTO**: 요청/응답 전용. `record` 타입 사용 권장.

### 계층 간 의존 방향
```
Controller → Service → Repository → Entity
              ↓
            Domain (다른 도메인 Service 호출 가능)
```
- Controller에서 Repository 직접 호출 금지
- Entity를 Controller 응답에 직접 노출 금지 (반드시 DTO 변환)

---

## 3. API 응답 포맷

### 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2025-01-15T10:30:00"
}
```

### 실패 응답
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "MEMBER_NOT_FOUND",
    "message": "해당 회원을 찾을 수 없습니다."
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

### 페이지네이션 응답
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  },
  "error": null,
  "timestamp": "2025-01-15T10:30:00"
}
```

---

## 4. 예외 처리

### 구조
```
BusinessException (abstract)
├── EntityNotFoundException       # 엔티티 조회 실패 (404)
├── DuplicateException            # 중복 데이터 (409)
├── InvalidStateException         # 잘못된 상태 전이 (400)
├── InsufficientTicketException   # 수강권 부족/만료 (400)
├── ReservationConflictException  # 예약 충돌 (409)
└── UnauthorizedException         # 권한 없음 (403)
```

### ErrorCode enum
- 도메인별 접두어: `MEMBER_`, `TICKET_`, `RESERVATION_`, `SCHEDULE_` 등
- 예: `MEMBER_NOT_FOUND`, `TICKET_EXPIRED`, `RESERVATION_CONFLICT`

### 처리 방식
- `@RestControllerAdvice` + `@ExceptionHandler`로 전역 예외 처리
- 예상된 비즈니스 예외: 적절한 HTTP 상태 코드 + ErrorCode
- 예상치 못한 예외: 500 + 일반 에러 메시지 (상세 내용은 로그에만)

---

## 5. 인증/인가

### JWT 토큰
- **Access Token**: 만료 30분, Authorization 헤더 (Bearer)
- **Refresh Token**: 만료 14일, HttpOnly Cookie
- Refresh Token은 Redis에 저장, 로그아웃 시 삭제
- Access Token 갱신: `/api/auth/refresh` 엔드포인트

### 역할 기반 접근 제어 (RBAC)
- `SUPER_ADMIN` > `ADMIN` > `INSTRUCTOR`
- 메서드 레벨: `@PreAuthorize("hasRole('ADMIN')")`
- 강사는 본인 수업/회원 정보만 조회 가능 (데이터 레벨 권한)

---

## 6. 로깅

### 저장 경로 및 롤링 정책
- **저장 경로**: `./logs/`
- **파일 구성**:
  - `application.log` — 기본 로그 (전체)
  - `request.log` — HTTP 요청/응답 전용
  - `error.log` — ERROR 레벨만 분리
- **롤링**: 일별 롤링 (`application-2026-05-08.log`)
- **보관 주기**: 3개월 (90일), 이후 자동 삭제
- **최대 파일 크기**: 단일 파일 100MB, 초과 시 분할 (`application-2026-05-08.0.log`)

### 요청/응답 (REQ/RES) 로깅
- **모든 API 요청/응답을 빠짐없이 기록** (LoggingFilter)
- 기록 항목:
  - `[REQ]` 타임스탬프, HTTP 메서드, URI, 클라이언트 IP, 요청자 ID, Request Body
  - `[RES]` 타임스탬프, HTTP 상태코드, 응답시간(ms), Response Body
- Request/Response Body는 최대 10KB까지 로깅 (초과 시 truncate)
- 파일 업로드 등 바이너리 요청은 메타 정보만 기록

### 핵심 로직 로깅
- **비즈니스 로직 수행 시 반드시 로그 작성**:
  - 예약 생성/취소/상태변경
  - 수강권 발급/차감/홀딩/해제
  - 회원 등록/수정/탈퇴
  - 결제/매출 처리
  - 인증 시도 (성공/실패)
- 포맷: `[도메인] [액션] 상세` (예: `[RESERVATION] [CREATE] memberId=123, lessonId=456`)

### 형식
- **Logback JSON 포맷** (운영 환경)
- 개발 환경: 콘솔 출력 (읽기 쉬운 형태)

### 개인정보 마스킹
- 운영/개발 환경: 마스킹 적용
  - 휴대폰 번호: `010-****-5678`
  - 이름: `김*수`
- **DEBUG 모드: 마스킹 해제** (로컬 디버깅 편의를 위해 원본 데이터 출력)
- 마스킹 on/off는 `application.yml`의 `logging.masking.enabled` 프로퍼티로 제어

### 로그 레벨별 출력
- `ERROR`: 예외 스택 트레이스 포함
- `WARN`: 비즈니스 규칙 위반 (취소 시간 초과 등)
- `INFO`: API 요청/응답 요약, 핵심 로직 수행 결과
- `DEBUG`: 상세 쿼리, 파라미터, 마스킹 해제된 원본 데이터 (로컬 환경만)

---

## 7. 프론트엔드 규칙

### 디렉토리 구조 (App Router)
```
src/
├── app/                       # Next.js App Router 페이지
│   ├── (auth)/                # 인증 관련 (로그인)
│   ├── (dashboard)/           # 인증 필요한 메인 레이아웃
│   │   ├── dashboard/
│   │   ├── members/
│   │   ├── tickets/
│   │   ├── reservations/
│   │   ├── instructors/
│   │   ├── revenue/
│   │   └── settings/
│   ├── layout.tsx
│   └── page.tsx
├── components/
│   ├── ui/                    # shadcn/ui 컴포넌트
│   └── domain/                # 도메인별 컴포넌트
│       ├── member/
│       ├── ticket/
│       ├── reservation/
│       └── schedule/
├── hooks/                     # 커스텀 훅
├── lib/                       # 유틸리티, API 클라이언트
├── stores/                    # Zustand 스토어
├── types/                     # TypeScript 타입 정의
└── styles/                    # 글로벌 스타일
```

### 컴포넌트 규칙
- **Server Components** 기본, 필요한 경우에만 `'use client'`
- 컴포넌트 파일명: PascalCase (`MemberList.tsx`)
- API 호출: React Query 훅으로 통일 (`useMemberList`, `useCreateMember`)
- 폼: React Hook Form + Zod 스키마 유효성 검증

### API 클라이언트
- Axios 인스턴스 (baseURL, interceptor로 JWT 자동 첨부)
- Access Token 만료 시 자동 갱신 (interceptor)
- API 에러 → React Query의 `onError`에서 토스트 알림

---

## 8. 데이터베이스 규칙

### 네이밍
- 테이블: `snake_case`, 복수형 (`members`, `tickets`, `reservations`)
- 컬럼: `snake_case` (`created_at`, `member_id`)
- 인덱스: `idx_{테이블}_{컬럼}` (`idx_reservations_lesson_date`)
- FK: `fk_{테이블}_{참조테이블}` (`fk_tickets_member`)

### 공통 컬럼 (모든 테이블)
- `id`: BIGINT, AUTO_INCREMENT, PK
- `created_at`: DATETIME, NOT NULL, DEFAULT CURRENT_TIMESTAMP
- `updated_at`: DATETIME, NOT NULL, ON UPDATE CURRENT_TIMESTAMP
- 논리 삭제: `deleted_at` (DATETIME, NULL) — 물리 삭제 지양

### 인덱스 및 쿼리 성능 관리
- **인덱스 원칙**: 모든 WHERE, JOIN, ORDER BY 대상 컬럼에 인덱스 검토 필수
- **쿼리 작성 시 점검 사항**:
  - EXPLAIN 실행하여 Full Table Scan 여부 확인
  - 복합 인덱스 컬럼 순서 검증 (선택도 높은 컬럼 우선)
  - 커버링 인덱스 활용 가능 여부 확인
- **금지 패턴**:
  - 인덱스 컬럼에 함수 적용 (`WHERE YEAR(created_at) = 2026`) → 범위 조건으로 변환
  - `SELECT *` 지양, 필요한 컬럼만 조회
  - N+1 쿼리 (fetch join 또는 `@EntityGraph`로 해결)
- **QueryDSL 작성 시**: 새 쿼리마다 EXPLAIN 결과를 리뷰하고, Full Scan이 발생하면 인덱스 추가 또는 쿼리 수정
- **슬로우 쿼리 모니터링**: MySQL slow_query_log 활성화 (1초 이상)

### JPA 설정
- `@CreatedDate`, `@LastModifiedDate` 활용 (JPA Auditing)
- `BaseEntity` 추상 클래스로 공통 필드 관리
- N+1 방지: fetch join 또는 `@EntityGraph` 사용, 복잡한 쿼리는 QueryDSL

---

## 9. 테스트 전략

| 레벨 | 도구 | 대상 | 커버리지 목표 |
|------|------|------|--------------|
| **단위 테스트** | JUnit 5 + Mockito | Service, Entity 비즈니스 로직 | 핵심 도메인 80% |
| **통합 테스트** | Testcontainers | Repository, API 엔드포인트 | 주요 API 100% |
| **E2E 테스트** | (2차) Playwright | 핵심 시나리오 | 주요 플로우 |

### 테스트 네이밍
- `@DisplayName("수강권 잔여 횟수가 0이면 예약할 수 없다")`
- 메서드명: `given_when_then` 패턴

### 테스트 자동화 프로세스
1. **도메인 개발 완료 시**: 해당 도메인의 단위 테스트 코드 자동 작성
2. **테스트 실행 → 실패 시**: 원인 분석 후 코드 자동 수정, 재실행하여 통과 확인
3. **검수 요청 시** (아래 순서로 진행):
   - 단위 테스트 전체 실행
   - 통합 테스트 전체 실행
   - 테스트 결과 분석 및 실패 항목 수정
   - 버그 의심 시나리오 도출 → 추가 테스트 코드 작성
   - 엣지 케이스/경계값 테스트 보강
   - 최종 전체 테스트 통과 확인 후 검수 완료 보고

### 핵심 테스트 시나리오 (반드시 포함)
- 예약 생성 시 수강권 검증
- 예약 취소 시 횟수 복구 / 페널티 차감
- 동시 예약 충돌 방지 (낙관적 락)
- 수강권 홀딩/해제 시 유효기간 계산

---

## 10. Git 컨벤션

### 커밋 메시지 (Conventional Commits, 한글 작성)
- **커밋 메시지는 한글로 상세히 작성한다.**
- 무엇을 왜 변경했는지 다른 사람이 읽고 바로 이해할 수 있어야 한다.

```
<type>(<scope>): <한글 제목>

<한글 본문 — 변경 사항 상세 설명>
- 무엇을 변경했는지
- 왜 변경했는지
- 영향 범위 또는 주의사항
```

### 예시
```
feat(member): 회원 CRUD API 구현

- 회원 등록/조회/수정/삭제 엔드포인트 추가
- 휴대폰 번호 중복 검증 로직 포함
- 검색 조건: 이름, 연락처, 상태 (QueryDSL)
- 페이지네이션 적용 (기본 20건)
```

```
fix(reservation): 동시 예약 시 수강권 이중 차감 버그 수정

- 낙관적 락(@Version) 적용하여 동시성 제어
- 충돌 시 ReservationConflictException 발생
- 기존: 두 요청 모두 성공 → 횟수 2회 차감
- 수정: 후속 요청은 409 Conflict 응답
```

### Type
| Type | 설명 |
|------|------|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드, 설정 변경 |
| `docs` | 문서 변경 |
| `style` | 코드 포맷팅 (기능 변경 없음) |

### Scope (도메인명)
`member`, `ticket`, `reservation`, `schedule`, `instructor`, `revenue`, `auth`, `infra`

### 브랜치 전략
- `main`: 배포 가능한 상태
- `develop`: 개발 통합 브랜치
- `feat/{domain}-{description}`: 기능 개발 (예: `feat/member-crud`)
- `fix/{description}`: 버그 수정
- `release/{version}`: 릴리스 준비

---

## 11. 보안

- **시크릿 관리**: 모든 시크릿은 환경변수 (`.env`), 코드에 하드코딩 금지
- **입력 검증**: Bean Validation (`@NotBlank`, `@Size`, `@Pattern` 등) 필수
- **SQL Injection**: JPA/QueryDSL 파라미터 바인딩으로 방지
- **XSS**: 프론트엔드 입력 sanitize + CSP 헤더
- **CORS**: 허용 도메인 명시적 설정
- **Rate Limiting**: 로그인 시도 5회/분 제한

### .gitignore 필수 항목
```
.env
.env.local
*.key
*.pem
application-secret.yml
```

---

## 12. 코드 스타일

### Java
- Google Java Style Guide 기반
- 들여쓰기: 4 spaces
- 최대 줄 길이: 120자
- `var` 사용: 타입이 명확한 경우에만

### TypeScript
- ESLint + Prettier
- 세미콜론 사용
- 작은따옴표 (`'`)
- 들여쓰기: 2 spaces

---

## 13. 환경 구분

| 환경 | 용도 | DB | 비고 |
|------|------|-----|------|
| `local` | 개발자 로컬 | Docker MySQL | Docker Compose |
| `dev` | 개발 서버 | 개발 DB | 자동 배포 |
| `prod` | 운영 | 운영 DB | 수동 배포 (1차) |

### 외부 설정 파일 (application.yml)
- **빌드 시 application.yml을 외부에서 교체/수정 가능하도록 구성**
- Spring Boot의 외부 설정 우선순위 활용:
  1. `./config/application.yml` (실행 경로의 config 디렉토리)
  2. `./application.yml` (실행 경로)
  3. classpath 내부 (jar 패키징)
- 배포 시 jar 파일 옆에 `config/` 디렉토리를 두고, 환경별 yml을 배치하여 내부 설정을 오버라이드
- 실행 예시:
  ```bash
  java -jar app.jar --spring.profiles.active=prod
  # 또는 외부 설정 파일 명시 지정
  java -jar app.jar --spring.config.location=file:./config/
  ```
- **시크릿 값**(DB 비밀번호, JWT 시크릿 등)은 환경변수 또는 외부 yml에서 주입, jar 내부에 절대 포함 금지
