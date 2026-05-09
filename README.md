# Pilates Studio - 예약 관리 시스템

소규모 필라테스 스튜디오를 위한 예약 관리 + 회원 관리 + 수강권 관리 웹 시스템.

## 빠른 시작 (5분)

```bash
# 1. Docker (MySQL + Redis)
docker-compose up -d

# 2. 백엔드
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
# http://localhost:8080 (Swagger: http://localhost:8080/swagger-ui.html)

# 3. 프론트엔드
cd frontend
npm install
npm run dev
# http://localhost:3000
```

**기본 관리자 계정:** `admin` / `admin1234`

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.3, JPA, QueryDSL, Flyway |
| Frontend | Next.js 16, React 19, TypeScript, TailwindCSS, shadcn/ui |
| Database | MySQL 8.0, Redis 7 |
| 인증 | JWT (Access 30분 + Refresh 14일) |
| 결제 | 토스페이먼츠 |
| 알림 | 카카오 알림톡 + SMS 폴백 |
| 테스트 | JUnit 5 + MockMvc (백엔드), Playwright (프론트엔드) |

## 프로젝트 구조

```
pilates-studio/
├── backend/          # Spring Boot API 서버
├── frontend/         # Next.js 웹 클라이언트
├── docs/             # 문서
│   ├── SPEC.md              # 확정 명세
│   ├── ARCHITECTURE.md      # 아키텍처 설계
│   ├── DB_DESIGN_DECISIONS.md
│   ├── OPERATION_MANUAL.md  # 운영 매뉴얼 (의뢰인용)
│   └── TROUBLESHOOTING.md   # 트러블슈팅 가이드
└── docker-compose.yml
```

## 도메인 (10개)

| 도메인 | 설명 | API 경로 |
|--------|------|----------|
| auth | SMS 인증, 회원가입, 로그인 | /api/auth/** |
| member | 회원 프로필, 탈퇴 | /api/members/** |
| instructor | 강사 관리, 근무시간 | /api/admin/instructors/** |
| classroom | 수업유형, 고정스케줄, 시간표 | /api/class-schedules/** |
| membership | 정기권 발급, 조회, 홀딩 | /api/memberships/** |
| payment | 결제, 환불 (토스) | /api/payments/** |
| reservation | 예약, 취소, 대기 | /api/reservations/** |
| attendance | 출석, 노쇼 | /api/instructor/attendances/** |
| notification | 알림톡, SMS | /api/notifications/** |
| admin | 대시보드, 통계, 엑셀 | /api/admin/** |

## 테스트

```bash
# 백엔드 (JUnit 5)
cd backend && ./gradlew clean test

# 프론트엔드 (Playwright)
cd frontend && npx playwright test
```
