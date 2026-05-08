# STEP 1: 백엔드 부트스트랩 작업 지시서

## 작업 일시
2026-05-08

## 원본 파일
`claude_code_초기세팅.txt` 내용을 그대로 보존.

---

STANDARDS.md와 PROJECT_STRUCTURE.md를 다시 읽고, 백엔드 부트스트랩을 진행한다.

[작업 범위]
backend/ 폴더에 Spring Boot 프로젝트를 생성하고, 도메인 코드는 만들지 말 것.
공통 인프라와 헬스체크까지만.

[기술 스택]
- Java 21
- Spring Boot 3.3.x (최신 안정 버전 사용)
- Gradle 8.x (Kotlin DSL, build.gradle.kts)
- 단일 모듈 (멀티모듈 X)
- JPA + Native Query
- MySQL 8 (운영), H2 (테스트)
- Redis (Spring Data Redis)
- Spring Security 6 (JWT 기반, 설정만 - 도메인 인증 로직은 나중)
- Spring Doc OpenAPI (Swagger UI)
- Lombok
- MapStruct (DTO 변환)
- Validation (Jakarta Bean Validation)

[생성할 것]
1. backend/build.gradle.kts, settings.gradle.kts
   - 의존성 정리 (group별 주석)
   - QueryDSL Q클래스 생성 설정
2. backend/src/main/resources/
   - application.yml (공통)
   - application-local.yml (개발용)
   - application-prod.yml (운영용, 환경변수 placeholder)
3. 공통 인프라 코드 (com.pilates.common 패키지)
   - ApiResponse<T> (success, data, error, timestamp)
   - ErrorCode enum (HTTP status, code, message)
   - BusinessException (abstract) + 도메인별 확장 가능 구조
   - GlobalExceptionHandler (@RestControllerAdvice)
   - 공통 BaseEntity (createdAt, updatedAt, createdBy, updatedBy - JPA Auditing)
   - PageResponse<T> (페이지네이션 응답 표준)
4. 설정 클래스 (com.pilates.config 패키지)
   - JpaConfig (Auditing 활성화)
   - QueryDslConfig (JPAQueryFactory Bean)
   - RedisConfig
   - SecurityConfig (CORS, 기본 필터 - JWT는 placeholder)
   - SwaggerConfig
   - WebConfig (CORS, Interceptor)
5. 로깅
   - logback-spring.xml (JSON 포맷, 개인정보 마스킹 패턴 - 휴대폰/이메일/주민번호)
6. 헬스체크
   - HealthController: GET /api/health → { status: "UP", timestamp, version }
   - Spring Boot Actuator 활성화 (/actuator/health, /actuator/info만 노출)
7. 인프라 (infra/ 폴더)
   - docker-compose.yml: MySQL 8 + Redis 7 (개발용)
   - .env.example
8. 메인 애플리케이션 클래스
   - PilatesApplication.java
9. 기본 테스트
   - PilatesApplicationTests (컨텍스트 로딩 확인)
   - HealthControllerTest (@WebMvcTest)

[중요 규칙]
- 도메인 코드 (member, reservation 등) 절대 생성 금지
- application.yml에 시크릿 하드코딩 금지, 환경변수 사용
- 모든 설정 클래스에 한국어 주석으로 의도 설명
- README.md (backend/) 작성: 로컬 실행 방법, 빌드, 테스트

[완료 후 출력]
1. 생성한 파일 트리
2. 다음 명령어로 로컬 실행 검증 가능한지 단계별 안내
   - docker-compose up -d
   - ./gradlew bootRun
   - curl http://localhost:8080/api/health
3. 주요 결정 사항 5줄 요약
4. 내가 추가로 결정해야 할 항목이 있으면 질문
