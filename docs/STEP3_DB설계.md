# STEP 3: DB 스키마 설계

## 작업 일시
2026-05-08 ~ 2026-05-09

## 작업 요약
SPEC.md 기반으로 전체 DB 스키마를 설계하고, JPA Entity 클래스와 Flyway 마이그레이션 SQL을 작성.
Service/Repository는 다음 단계에서 구현.

## 완료 항목

### 문서
- `docs/ERD.md` — 전체 ERD (mermaid), 19개 테이블, 도메인 그룹 요약, 관계 정리
- `docs/erd/` — 도메인별 분리 ERD 5개 파일
  - ERD_member.md, ERD_classroom.md, ERD_reservation.md, ERD_payment.md, ERD_admin.md
- `docs/DB_DESIGN_DECISIONS.md` — 주요 설계 결정 11개 항목 + 회색 지대 기록

### Entity 클래스 (31개 파일)

| 도메인 | Entity | Enum |
|--------|--------|------|
| member | Member, MemberMemo, WithdrawnMemberLog | MemberStatus, Gender |
| instructor | Instructor, InstructorAvailableTime | InstructorStatus |
| classroom | LessonType, FixedSchedule, ClassSchedule | ClassScheduleStatus |
| membership | Membership, MembershipHolding, MembershipLessonType | MembershipStatus |
| reservation | Reservation | ReservationStatus |
| attendance | Attendance | AttendanceStatus |
| payment | Payment | PaymentMethod, PaymentStatus |
| notification | Notification | NotificationType, NotificationStatus |
| admin | Admin, AdminAuditLog, StudioSetting, Holiday | AdminRole |

### Flyway 마이그레이션
- `V1__init_schema.sql` — 19개 테이블 생성 (모든 환경 공통)
- `V2__add_indexes.sql` — 30개 인덱스 (모든 환경 공통)
- `db/seed/dev/R__dev_seed.sql` — 개발 시드 (Flyway repeatable)
- `db/seed/prod/prod_init.sql` — 운영 초기 세팅 (수동 1회 실행)
- `db/seed/prod/README.md` — 운영 실행 가이드

### 설정 변경
- `build.gradle.kts` — Flyway 의존성 추가 (flyway-core, flyway-mysql)
- `application.yml` — Flyway 공통 설정 추가
- `application-local.yml` — flyway.locations: migration + seed/dev
- `application-local-h2.yml` — flyway.locations: migration + seed/dev, ddl-auto: none
- `application-prod.yml` — flyway.locations: migration만, Swagger 비활성화
- `application-test.yml` — Flyway 비활성화

## 주요 설계 결정

### 1. Soft Delete + UNIQUE 충돌 해결
- 회원 탈퇴 시 `phone_hash`를 NULL로 변경 (MySQL UNIQUE는 NULL 중복 허용)
- 원본 개인정보는 `withdrawn_member_logs`에 보관, 30일 후 익명화

### 2. 정기권-수업 유형 유연 매핑
- `membership_lesson_types` 중간 테이블로 1:N 매핑
- 겸용 정기권(개인+듀엣) 대응 가능

### 3. 동시성 처리
- 정기권 잔여 횟수: 비관적 락 (`@Lock(PESSIMISTIC_WRITE)`)
- 수업 정원: 낙관적 락 (`@Version` + `current_count`)
- 결제 중복: `order_id` UNIQUE 제약

### 4. Flyway 시드 분리
- V1, V2는 모든 환경 공통
- 개발 시드: `db/seed/dev/` (Flyway repeatable, 자동 적용)
- 운영 초기 세팅: `db/seed/prod/` (수동 실행)

### 5. Entity 설계 원칙 (7가지 실수 방지 검증 완료)
- 양방향 연관관계 없음 — 모두 단방향
- cascade 없음 — 명시적 필요 시만 추가
- 모든 @ManyToOne, @OneToOne에 `fetch = FetchType.LAZY` 명시
- 모든 @Enumerated에 `EnumType.STRING` 사용
- Auditing은 `@CreatedDate` 통일 (`@CreationTimestamp` 사용 금지)
- 금액은 `BigDecimal` + `DECIMAL(10,0)`
- 검색용 컬럼(phone_hash, public_id) UNIQUE 인덱스

## 회색 지대 (STEP 4 전 결정)

| # | 항목 | 현재 상태 |
|---|------|-----------|
| 1 | 암호화 키 관리 (AES 키 보관) | placeholder |
| 3 | 수업 자동 생성 트리거 (배치 방식) | placeholder |
| 4 | 노쇼 자동 처리 + 정기권 차감 시점 | placeholder |
| 7 | 결제 PG 연동 범위 | order_id로 자체/PG 모두 대응 가능 |

## 검증 결과
- BUILD SUCCESSFUL (2 tests passed)
- Flyway: `Successfully applied 3 migrations to schema "PUBLIC"` (V1 + V2 + R__dev_seed)
- 헬스체크: http://localhost:8080/api/health → 200 OK
- H2 콘솔: 19개 테이블 정상 생성 확인

## Git
- 커밋: `7ee01c6` — feat(infra): DB 스키마 설계 및 도메인 엔티티, Flyway 마이그레이션 추가
