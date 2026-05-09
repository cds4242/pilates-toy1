# Pilates Studio Backend

필라테스 스튜디오 예약 관리 시스템 — Spring Boot API 서버

## 기술 스택
- Java 21 + Spring Boot 3.3.7
- Spring Data JPA + QueryDSL 5.1
- MySQL 8.0 + Redis 7
- Gradle 8.10.2 (Kotlin DSL)

## 프로파일 (중요)

**application.yml에 기본 프로파일이 없다.** 실행 시 반드시 명시해야 한다.
프로파일 누락 시 datasource 설정이 없어 컨텍스트 로딩이 실패한다.

| 프로파일 | 용도 | DB | Redis |
|----------|------|-----|-------|
| `local-h2` | Docker 없이 로컬 개발 | H2 인메모리 | 비활성화 |
| `local` | Docker 기반 로컬 개발 | MySQL (Docker) | Redis (Docker) |
| `prod` | 운영 | MySQL (환경변수) | Redis (환경변수) |

> `./gradlew bootRun`은 기본적으로 `local-h2`로 실행된다 (build.gradle.kts 설정).
> JAR 직접 실행 시에는 반드시 `--spring.profiles.active` 를 명시해야 한다.

## 로컬 실행 방법

### A. Docker 없이 (H2 인메모리)
```bash
cd backend
./gradlew bootRun
```
- `bootRun`은 자동으로 `local-h2` 프로파일 적용
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:pilates`, user: `sa`)

### B. Docker 기반 (MySQL + Redis)
```bash
cd infra && docker-compose up -d
cd ../backend
./gradlew bootRun -Dspring.profiles.active=local
```

### 3. 헬스체크 확인
```bash
curl http://localhost:8080/api/health
```

### 4. Swagger UI
```
http://localhost:8080/swagger-ui.html
```

## 빌드
```bash
./gradlew build
```

## 테스트
```bash
# 전체 테스트 (H2 인메모리 DB, Docker 불필요)
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.pilates.common.controller.HealthControllerTest"
```

## 운영 빌드 및 실행
```bash
# JAR 빌드
./gradlew bootJar

# 실행 (외부 설정 파일 사용)
java -jar build/libs/pilates-studio-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.config.location=file:./config/
```

## 환경변수 (운영)
| 변수 | 설명 |
|------|------|
| `DB_HOST` | MySQL 호스트 |
| `DB_PORT` | MySQL 포트 (기본 3306) |
| `DB_NAME` | 데이터베이스명 |
| `DB_USERNAME` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `REDIS_HOST` | Redis 호스트 |
| `REDIS_PORT` | Redis 포트 (기본 6379) |
| `REDIS_PASSWORD` | Redis 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 (최소 32자) |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 도메인 |

## 권한 시스템

4개 역할로 API 접근을 분리한다.

| 역할 | 로그인 경로 | 접근 가능 API |
|------|------------|--------------|
| MEMBER | `POST /api/auth/login` | `/api/members/me/**`, `/api/reservations/**` |
| INSTRUCTOR | `POST /api/admin/auth/login` | `/api/instructor/**`, `/api/admin/**` |
| ADMIN | `POST /api/admin/auth/login` | `/api/admin/**` |
| SUPER_ADMIN | `POST /api/admin/auth/login` | `/api/admin/**` |

- 자세한 권한 매트릭스: `docs/ARCHITECTURE.md` 7장 참고
- 테스트 시 토큰 발급: `AuthTestHelper` 활용 (`src/test/java/.../integration/support/`)
