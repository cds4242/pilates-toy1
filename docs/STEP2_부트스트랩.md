# STEP 2: 백엔드 부트스트랩 + 환경 세팅

## 작업 일시
2026-05-08

## 작업 요약
Spring Boot 백엔드 프로젝트 초기화. 도메인 코드 없이 공통 인프라와 헬스체크까지만 구성.

## 완료 항목

### 환경 설치
| 도구 | 버전 | 비고 |
|------|------|------|
| Java (Oracle JDK) | 21.0.11 LTS | winget 설치 |
| Node.js | v24.15.0 | winget 설치 |
| Git | 2.53.0 | 기존 설치 |
| GitHub CLI | 2.92.0 | winget 설치 |
| pnpm | npm -g | Node 설치 후 추가 |
| Docker Desktop | 4.71.0 | 설치됨, 재부팅 후 사용 가능 |

### 프로젝트 문서
- `docs/SPEC.md` — 의뢰인 확정 명세
- `STANDARDS.md` — 기술 표준 (로깅, DB 인덱스, 테스트 자동화, 외부 설정 등)
- `PROJECT_STRUCTURE.md` — 전체 폴더 구조

### 백엔드 부트스트랩
- **기술 스택**: Java 21 + Spring Boot 3.3.7 + Gradle 8.10.2 (Kotlin DSL)
- **패키지**: `com.pilates` (common/config/domain 분리)
- **공통 인프라**:
  - `ApiResponse<T>` — 통일된 API 응답 래퍼
  - `PageResponse<T>` — 페이지네이션 표준
  - `ErrorCode` enum — 도메인별 확장 가능 (COMMON_, MEMBER_, TICKET_ 등)
  - `BusinessException` (abstract) + EntityNotFoundException, InvalidStateException, DuplicateException
  - `GlobalExceptionHandler` — Validation, AccessDenied, Authentication, 비즈니스 예외 전부 처리
  - `BaseEntity` — id, createdAt, updatedAt, createdBy, updatedBy, deletedAt (논리삭제)
- **설정 클래스**:
  - JpaConfig (Auditing), QueryDslConfig, RedisConfig (@ConditionalOnBean), SecurityConfig, SwaggerConfig, WebConfig (CORS)
- **로깅**:
  - logback-spring.xml — ./logs/ 저장, 일별 롤링, 3개월 보관
  - LoggingFilter — REQ/RES 전체 기록, 마스킹 on/off
  - MaskingUtil — 휴대폰, 이메일, 주민번호 정규식 마스킹
- **헬스체크**: GET /api/health → { status: "UP", version, timestamp }
- **Actuator**: health, info만 노출

### 프로파일 전략
| 프로파일 | DB | Redis | 용도 |
|----------|-----|-------|------|
| `local-h2` | H2 인메모리 | 비활성화 | Docker 없이 개발 |
| `local` | MySQL (Docker) | Redis (Docker) | Docker 기반 개발 |
| `prod` | MySQL (환경변수) | Redis (환경변수) | 운영 |
| `test` | H2 인메모리 | 비활성화 | 테스트 |

- application.yml에 **기본 프로파일 없음** (누락 시 실패 → 운영 사고 방지)
- `./gradlew bootRun`은 build.gradle.kts에서 `local-h2` 자동 적용
- 운영 Swagger UI 비활성화 (prod 프로파일)

### 인프라
- `infra/docker-compose.yml` — MySQL 8 + Redis 7 (로컬 개발용)
- `infra/.env.example` — 환경변수 템플릿
- `infra/mysql/init.sql` — 슬로우 쿼리 로그 활성화
- `infra/mysql/my.cnf` — utf8mb4, 타임존 설정

### 테스트
- `PilatesApplicationTests` — 컨텍스트 로딩 확인 (H2, Redis 제외)
- `HealthControllerTest` — @WebMvcTest + SecurityConfig Import, permitAll 검증
- **BUILD SUCCESSFUL**, 2 tests passed

### 보안 검수 완료
- application.yml에 시크릿 하드코딩 없음 (prod는 환경변수)
- .gitignore에 .env, *.log, logs/, build/ 포함
- Swagger UI 운영 비활성화
- AccessDeniedException, AuthenticationException 핸들러 추가

### 공통규칙 변경사항
- STANDARDS.md → "공통규칙"으로 명명 변경
- **Git 커밋 메시지는 한글로 상세히 작성** (무엇을, 왜, 영향 범위 포함)
- Conventional Commits 형식 유지하되 제목/본문 모두 한글

### Git
- GitHub repo: https://github.com/cds4242/pilates-system (private)
- 커밋: `af29c83` — feat: bootstrap backend with spring boot, common infra, health check
- 브랜치: master

## 실행 방법
```bash
cd backend
JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" ./gradlew bootRun

# 헬스체크
curl http://localhost:8080/api/health

# H2 콘솔
# http://localhost:8080/h2-console (URL: jdbc:h2:mem:pilates, user: sa, pw: 빈칸)

# Swagger UI
# http://localhost:8080/swagger-ui.html
```

## 다음 단계
- [ ] 도메인 개발 시작 (Member, Ticket, Reservation 등)
- [ ] ERD 설계
- [ ] Docker Desktop 재부팅 후 MySQL/Redis 연동 확인
