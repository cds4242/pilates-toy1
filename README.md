# Pilates Studio · 예약 관리 시스템

소규모 필라테스 스튜디오를 위한 예약 · 회원 · 수강권 · 결제 · 알림 통합 관리 웹 시스템.

> 🎬 **시연 모드(demo 프로파일)**: 외부 결제·SMS·카카오 알림톡은 Mock으로 동작합니다. 실 결제·발송 X.

## 🌐 Live Demo

| 단계 | 환경 | URL | 상태 |
|---|---|---|---|
| 1단계 | NAS 박제 (정적 export) | https://dsjh.synology.me:8443/p1 | 동결 |
| 2단계 | Railway 풀스택 | https://frontend-production-8081.up.railway.app | 활성 |

**Backend API**: https://backend-production-81c77.up.railway.app

> Railway Trial Workspace에서 운영 중 ([D-014](./DECISIONS.md)). 크레딧 소진 시 일시 정지될 수 있음.
> 시연용 계정은 아래 **[시연용 테스트 계정](#-시연용-테스트-계정)** 참조.

## 📚 Tech Stack
- **Backend**: Spring Boot 3 · Java 21 · MySQL 8 · Redis 7
- **Frontend**: Next.js 16 · React 19 · TypeScript · TailwindCSS · Pretendard
- **Infrastructure**: Docker · Railway · GitHub Actions
- **Architecture**: Monorepo · 12-Factor App · Multi-stage Build

## 📊 Tests & Quality
- Backend: 118 / 121 tests passed (인프라 의존 2건 제외)
- Frontend: Playwright 14 visual tests
- CI/CD: GitHub Actions (auto build & test)

## 🛠 Documentation
- [개발 회고 (WORKLOG)](./WORKLOG.md)
- [의사결정 기록 (DECISIONS)](./DECISIONS.md)
- [미완료 항목 (DEFERRED)](./DEFERRED_ITEMS.md)
- 핸드오버 체크리스트: `docs/HANDOVER_CHECKLIST.md`

---

## 🚀 빠른 시작

### 옵션 A — Docker Compose 풀스택 (권장)

```bash
# 1) 환경변수 준비 (한 번만)
cp .env.example .env
# .env 안의 change_me_* 값을 채우거나, 로컬 검증용 더미로 둬도 동작

# 2) 빌드 + 기동 (백엔드 + 프론트엔드 + MySQL + Redis)
docker compose -f docker-compose.prod.yml --env-file .env up -d --build

# 3) 접속
# 프론트엔드:  http://localhost:3000
# 백엔드 API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### 옵션 B — 로컬 개발 모드

```bash
# 인프라만 Docker로
docker compose up -d mysql redis

# 백엔드 (local 프로파일 — DB는 Docker MySQL)
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드
cd ../frontend
npm install
npm run dev
```

### 옵션 C — DB 없이 단독 실행 (H2 인메모리)

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local-h2'
```

---

## 🔑 시연용 테스트 계정

> 모든 비밀번호: **`demo1234`**

### 관리자

로그인 페이지: `http://localhost:3000/admin-login`

| 아이디 | 이름 | 역할 | 비고 |
|---|---|---|---|
| `admin_demo` | 데모관리자 | SUPER_ADMIN | 시연 메인 계정 — 모든 메뉴 접근 |
| `admin` | 관리자 | SUPER_ADMIN | 기본 초기 계정 |

### 강사

로그인 페이지: `http://localhost:3000/instructor-login`

| 아이디 | 이름 | 비고 |
|---|---|---|
| `instructor_demo` | 박데모 | 시연 메인 강사 — 담당 수업 + 예약자 풍부 |
| `instructor1` | 박지영 | 주력 강사 · 그룹/개인 |
| `instructor2` | 이수진 | 재활 전문 · 월/수/금 |
| `instructor3` | 최재훈 | 체형교정 · 화/목/토 |
| `instructor4`~`9` | 김하늘 외 | 기타 강사 |

### 회원

로그인 페이지: `http://localhost:3000/login`

| 전화번호 | 이름 | 보유 정기권 | 비고 |
|---|---|---|---|
| `010-0000-0001` | 김데모 | 12회권 (잔여 9/12) | 시연 메인 회원 · 미래 예약 + 과거 출석 풍부 |

> 그 외 시드 회원은 관리자 페이지에서 76명 일괄 조회 가능 (전화번호로 직접 로그인 불가 — 비밀번호 미설정).

---

## 🛠️ 기술 스택

| 영역 | 사용 기술 |
|---|---|
| Backend | Java 21 · Spring Boot 3.3 · JPA · QueryDSL · Flyway |
| Frontend | Next.js 16 · React 19 · TypeScript · TailwindCSS |
| Database | MySQL 8.0 · Redis 7 |
| 인증 | JWT (Access 30분 + Refresh 14일) |
| 결제 | 토스페이먼츠 (demo 프로파일에서는 Mock) |
| 알림 | 카카오 알림톡 (NHN Toast) + SMS 폴백 (demo에서는 Mock) |
| 빌드 | Gradle 8.10 · npm |
| 배포 | Docker Compose · Railway (예정) |
| 테스트 | JUnit 5 + MockMvc + Testcontainers · Playwright |

---

## 📁 프로젝트 구조

```
pilates-toy1/
├── backend/                   # Spring Boot API 서버
│   └── src/main/resources/
│       ├── application.yml           # 공통
│       ├── application-local.yml     # 로컬 (Docker MySQL)
│       ├── application-local-h2.yml  # 로컬 (H2 인메모리)
│       ├── application-portfolio.yml # NAS 박제 시연
│       ├── application-demo.yml      # Railway·풀스택 시연 (Mock 통합)
│       └── application-prod.yml      # 실 운영 (실 키 필요)
├── frontend/                  # Next.js 웹 클라이언트
├── infra/                     # MySQL my.cnf 등 인프라 설정
├── nas-snapshot/              # NAS 박제본 (참고 보존, D-008 동결)
├── docs/                      # 명세·아키텍처·운영 매뉴얼
├── docker-compose.yml         # 로컬 개발용 (MySQL + Redis만)
├── docker-compose.prod.yml    # 풀스택 + Mock 통합 (demo 프로파일)
├── SPEC.md
├── STANDARDS.md
├── DECISIONS.md               # 주요 의사결정 (D-001 ~ D-013)
├── WORKLOG.md                 # 세션 간 작업 로그
└── DEFERRED_ITEMS.md          # 알면서 미루는 작업
```

---

## 🧩 도메인 (10개)

| 도메인 | 책임 | 주요 API |
|---|---|---|
| `auth` | SMS 인증 · 회원가입 · 로그인 | `/api/auth/**` |
| `member` | 회원 프로필 · 탈퇴 · 메모 | `/api/admin/members/**` |
| `instructor` | 강사 관리 · 근무시간 | `/api/admin/instructors/**` |
| `classroom` | 수업 유형 · 시간표 | `/api/admin/class-schedules/**` |
| `membership` | 정기권 발급 · 홀딩 · 조회 | `/api/admin/memberships/**` |
| `payment` | 토스 결제 · 환불 | `/api/payments/**` |
| `reservation` | 예약 · 취소 · 대기 | `/api/reservations/**` |
| `attendance` | 출석 · 노쇼 | `/api/instructor/attendances/**` |
| `notification` | 알림톡 · SMS | `/api/notifications/**` |
| `admin` | 대시보드 · 통계 · 엑셀 | `/api/admin/**` |

---

## 🧭 Spring 프로파일

| 프로파일 | DB | 외부 통합 | 시드 | 용도 |
|---|---|---|---|---|
| `local` | MySQL (Docker) | Mock | ✅ | 개발자 PC |
| `local-h2` | H2 (인메모리) | Mock | ✅ | DB 없이 단독 실행 |
| `test` | H2 | Mock | — | 자동화 테스트 |
| `portfolio` | H2 + 임베디드 Redis | Mock | ✅ | NAS 박제 (단독 JAR) |
| `demo` | MySQL + Redis | **Mock** | ✅ | **Railway 풀스택 시연** |
| `prod` | MySQL + Redis | 실 통합 | — | 실 운영 (실 키 필요) |

`docker-compose.prod.yml`은 `demo` 프로파일을 사용합니다. 실 운영은 환경변수 `SPRING_PROFILES_ACTIVE=prod` + 실 NHN Toast/토스 키 주입.

---

## 🧪 테스트 실행

```bash
# 백엔드 (JUnit 5) — H2 기반
cd backend && ./gradlew clean test

# 프론트엔드 (Playwright)
cd frontend && npx playwright test
```

---

## 📚 추가 문서

- [`SPEC.md`](./SPEC.md) — 확정 기능 명세
- [`DECISIONS.md`](./DECISIONS.md) — 주요 의사결정 로그
- [`WORKLOG.md`](./WORKLOG.md) — 세션 작업 기록
- [`DEFERRED_ITEMS.md`](./DEFERRED_ITEMS.md) — 보류 항목
- [`docs/`](./docs/) — SPEC · ARCHITECTURE · OPERATION_MANUAL · TROUBLESHOOTING
