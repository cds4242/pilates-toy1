# 프로젝트 폴더 구조 (PROJECT_STRUCTURE.md)

## 최상위 구조

```
pilates-system/
├── backend/                # Spring Boot 백엔드 애플리케이션
├── frontend/               # Next.js 프론트엔드 애플리케이션
├── infra/                  # Docker, 배포, 인프라 설정
├── docs/                   # 프로젝트 문서 (명세, 회의록 등)
├── STANDARDS.md            # 기술 표준 문서
├── PROJECT_STRUCTURE.md    # 이 파일
├── .gitignore
└── README.md
```

| 폴더 | 역할 |
|------|------|
| `backend/` | Java 21 + Spring Boot 3.x API 서버. Gradle 멀티 모듈은 사용하지 않고 단일 모듈로 시작. |
| `frontend/` | Next.js 14 App Router 기반 관리자 웹 SPA. |
| `infra/` | Docker Compose, Dockerfile, Nginx 설정, DB 마이그레이션 스크립트 등. |
| `docs/` | 의뢰인 명세(SPEC.md), API 설계, ERD, 회의록 등 프로젝트 문서. |

---

## backend/ 내부 구조

```
backend/
├── build.gradle.kts                  # Gradle 빌드 설정
├── settings.gradle.kts
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/pilates/studio/
    │   │   ├── PilatesStudioApplication.java
    │   │   │
    │   │   ├── global/                        # ── 전역 공통 ──
    │   │   │   ├── config/
    │   │   │   │   ├── JpaConfig.java         # JPA Auditing, QueryDSL
    │   │   │   │   ├── SecurityConfig.java    # Spring Security
    │   │   │   │   ├── RedisConfig.java
    │   │   │   │   ├── WebConfig.java         # CORS, 인터셉터
    │   │   │   │   └── SwaggerConfig.java     # OpenAPI
    │   │   │   │
    │   │   │   ├── error/
    │   │   │   │   ├── BusinessException.java
    │   │   │   │   ├── ErrorCode.java         # enum
    │   │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   │   └── exceptions/            # 구체적 예외 클래스
    │   │   │   │
    │   │   │   ├── response/
    │   │   │   │   └── ApiResponse.java       # 공통 응답 래퍼
    │   │   │   │
    │   │   │   ├── security/
    │   │   │   │   ├── jwt/
    │   │   │   │   │   ├── JwtTokenProvider.java
    │   │   │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   │   │   └── JwtProperties.java
    │   │   │   │   └── auth/
    │   │   │   │       ├── CustomUserDetails.java
    │   │   │   │       └── CustomUserDetailsService.java
    │   │   │   │
    │   │   │   ├── entity/
    │   │   │   │   └── BaseEntity.java        # id, createdAt, updatedAt
    │   │   │   │
    │   │   │   ├── util/
    │   │   │   │   └── MaskingUtil.java       # 개인정보 마스킹
    │   │   │   │
    │   │   │   └── logging/
    │   │   │       └── LoggingFilter.java     # 요청/응답 로깅
    │   │   │
    │   │   └── domain/                        # ── 도메인 모듈 ──
    │   │       │
    │   │       ├── member/
    │   │       │   ├── controller/
    │   │       │   │   └── MemberController.java
    │   │       │   ├── service/
    │   │       │   │   └── MemberService.java
    │   │       │   ├── repository/
    │   │       │   │   ├── MemberRepository.java        # JPA
    │   │       │   │   └── MemberQueryRepository.java   # QueryDSL
    │   │       │   ├── entity/
    │   │       │   │   └── Member.java
    │   │       │   └── dto/
    │   │       │       ├── MemberCreateRequest.java
    │   │       │       ├── MemberUpdateRequest.java
    │   │       │       └── MemberResponse.java
    │   │       │
    │   │       ├── ticket/
    │   │       │   ├── controller/
    │   │       │   ├── service/
    │   │       │   ├── repository/
    │   │       │   ├── entity/
    │   │       │   │   ├── Ticket.java
    │   │       │   │   ├── TicketType.java              # enum
    │   │       │   │   └── TicketStatus.java            # enum
    │   │       │   └── dto/
    │   │       │
    │   │       ├── reservation/
    │   │       │   ├── controller/
    │   │       │   ├── service/
    │   │       │   ├── repository/
    │   │       │   ├── entity/
    │   │       │   │   ├── Reservation.java
    │   │       │   │   └── ReservationStatus.java       # enum
    │   │       │   └── dto/
    │   │       │
    │   │       ├── schedule/
    │   │       │   ├── controller/
    │   │       │   ├── service/
    │   │       │   ├── repository/
    │   │       │   ├── entity/
    │   │       │   └── dto/
    │   │       │
    │   │       ├── lesson/
    │   │       │   ├── controller/
    │   │       │   ├── service/
    │   │       │   ├── repository/
    │   │       │   ├── entity/
    │   │       │   │   ├── Lesson.java
    │   │       │   │   └── LessonType.java              # enum
    │   │       │   └── dto/
    │   │       │
    │   │       ├── instructor/
    │   │       │   ├── controller/
    │   │       │   ├── service/
    │   │       │   ├── repository/
    │   │       │   ├── entity/
    │   │       │   └── dto/
    │   │       │
    │   │       ├── revenue/
    │   │       │   ├── controller/
    │   │       │   ├── service/
    │   │       │   ├── repository/
    │   │       │   ├── entity/
    │   │       │   └── dto/
    │   │       │
    │   │       ├── notification/                # (2차 MVP)
    │   │       │   ├── service/
    │   │       │   ├── entity/
    │   │       │   └── dto/
    │   │       │
    │   │       └── auth/
    │   │           ├── controller/
    │   │           │   └── AuthController.java
    │   │           ├── service/
    │   │           │   └── AuthService.java
    │   │           └── dto/
    │   │               ├── LoginRequest.java
    │   │               └── TokenResponse.java
    │   │
    │   └── resources/
    │       ├── application.yml                  # 공통 설정
    │       ├── application-local.yml            # 로컬 환경
    │       ├── application-dev.yml              # 개발 환경
    │       ├── application-prod.yml             # 운영 환경
    │       ├── logback-spring.xml               # 로깅 설정
    │       └── data.sql                         # 초기 데이터 (개발용)
    │
    └── test/
        └── java/com/pilates/studio/
            ├── domain/
            │   ├── member/
            │   │   ├── service/
            │   │   │   └── MemberServiceTest.java
            │   │   └── repository/
            │   │       └── MemberRepositoryTest.java
            │   ├── ticket/
            │   │   └── service/
            │   │       └── TicketServiceTest.java
            │   └── reservation/
            │       └── service/
            │           └── ReservationServiceTest.java
            └── integration/
                └── ReservationIntegrationTest.java
```

---

## frontend/ 내부 구조

```
frontend/
├── package.json
├── pnpm-lock.yaml
├── next.config.js
├── tsconfig.json
├── tailwind.config.ts
├── postcss.config.js
├── Dockerfile
├── .env.local                              # 환경변수 (gitignore)
├── .env.example                            # 환경변수 템플릿
│
├── public/
│   └── images/
│
└── src/
    ├── app/                                # ── App Router ──
    │   ├── layout.tsx                      # 루트 레이아웃
    │   ├── page.tsx                        # 루트 → 로그인 리다이렉트
    │   │
    │   ├── (auth)/                         # 비인증 그룹
    │   │   ├── layout.tsx
    │   │   └── login/
    │   │       └── page.tsx
    │   │
    │   └── (dashboard)/                    # 인증 필요 그룹
    │       ├── layout.tsx                  # 사이드바 + 헤더 레이아웃
    │       ├── dashboard/
    │       │   └── page.tsx                # 대시보드 홈
    │       ├── members/
    │       │   ├── page.tsx                # 회원 목록
    │       │   ├── new/
    │       │   │   └── page.tsx            # 회원 등록
    │       │   └── [id]/
    │       │       └── page.tsx            # 회원 상세
    │       ├── tickets/
    │       │   ├── page.tsx                # 수강권 목록
    │       │   └── new/
    │       │       └── page.tsx            # 수강권 발급
    │       ├── reservations/
    │       │   └── page.tsx                # 주간 캘린더 (예약 관리)
    │       ├── instructors/
    │       │   ├── page.tsx                # 강사 목록
    │       │   └── [id]/
    │       │       └── page.tsx            # 강사 상세
    │       ├── revenue/
    │       │   └── page.tsx                # 매출 대시보드
    │       └── settings/
    │           └── page.tsx                # 설정
    │
    ├── components/
    │   ├── ui/                             # shadcn/ui (자동 생성)
    │   │   ├── button.tsx
    │   │   ├── input.tsx
    │   │   ├── dialog.tsx
    │   │   ├── table.tsx
    │   │   ├── calendar.tsx
    │   │   └── ...
    │   ├── layout/                         # 레이아웃 컴포넌트
    │   │   ├── Sidebar.tsx
    │   │   ├── Header.tsx
    │   │   └── PageTitle.tsx
    │   └── domain/                         # 도메인별 컴포넌트
    │       ├── member/
    │       │   ├── MemberList.tsx
    │       │   ├── MemberForm.tsx
    │       │   └── MemberDetail.tsx
    │       ├── ticket/
    │       │   ├── TicketList.tsx
    │       │   ├── TicketForm.tsx
    │       │   └── TicketStatusBadge.tsx
    │       ├── reservation/
    │       │   ├── WeeklyCalendar.tsx
    │       │   ├── ReservationSlot.tsx
    │       │   └── ReservationDialog.tsx
    │       └── schedule/
    │           └── ScheduleGrid.tsx
    │
    ├── hooks/                              # 커스텀 훅
    │   ├── api/                            # React Query 훅
    │   │   ├── useMemberList.ts
    │   │   ├── useCreateMember.ts
    │   │   ├── useTicketList.ts
    │   │   ├── useReservations.ts
    │   │   └── ...
    │   ├── useAuth.ts
    │   └── useToast.ts
    │
    ├── lib/                                # 유틸리티
    │   ├── api/
    │   │   ├── client.ts                   # Axios 인스턴스 + 인터셉터
    │   │   └── endpoints.ts                # API 엔드포인트 상수
    │   ├── utils.ts                        # 범용 유틸
    │   └── validations/                    # Zod 스키마
    │       ├── member.ts
    │       ├── ticket.ts
    │       └── reservation.ts
    │
    ├── stores/                             # Zustand 스토어
    │   ├── authStore.ts
    │   └── uiStore.ts                      # 사이드바 상태 등
    │
    ├── types/                              # 타입 정의
    │   ├── member.ts
    │   ├── ticket.ts
    │   ├── reservation.ts
    │   ├── instructor.ts
    │   ├── api.ts                          # ApiResponse 등 공통 타입
    │   └── auth.ts
    │
    └── styles/
        └── globals.css                     # TailwindCSS 기본 + 커스텀
```

---

## infra/ 내부 구조

```
infra/
├── docker-compose.yml                      # 로컬 개발 환경 (MySQL, Redis, 앱)
├── docker-compose.prod.yml                 # 운영 환경 (추후)
├── mysql/
│   ├── init.sql                            # DB 초기화 (스키마, 초기 데이터)
│   └── my.cnf                              # MySQL 설정 (charset 등)
├── redis/
│   └── redis.conf                          # Redis 설정
├── nginx/
│   └── default.conf                        # 리버스 프록시 (추후)
└── .env.example                            # 인프라 환경변수 템플릿
```

---

## docs/ 내부 구조

```
docs/
├── SPEC.md                                 # 의뢰인 확정 명세
├── ERD.md                                  # 데이터베이스 ERD (추후 작성)
├── API.md                                  # API 설계 문서 (추후 작성)
└── CHANGELOG.md                            # 변경 이력 (추후 작성)
```
