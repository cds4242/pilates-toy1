# DB 설계 결정 사항

## 1. ID 전략

### 내부 ID: BIGINT AUTO_INCREMENT
- 모든 테이블의 PK는 `BIGINT AUTO_INCREMENT`
- 내부 조인, FK 참조에 사용

### 외부 ID: public_id (VARCHAR(32))
- 회원, 강사, 정기권 등 **외부에 노출되는 엔티티**에만 `public_id` 추가
- UUID v4 기반, 하이픈 제거 32자 (`550e8400e29b41d4a716446655440000`)
- 이유: 내부 auto_increment ID를 URL/API에 노출하면 추측 공격 가능

---

## 2. Soft Delete

### 대상 테이블
- `members`, `instructors`, `memberships`, `reservations`, `admins`
- `deleted_at TIMESTAMP NULL` — NULL이면 활성, 값이 있으면 삭제

### Soft Delete + UNIQUE 제약 충돌 해결

**회원 탈퇴 시 phone_hash 처리 (결정: 2026-05-08)**
- 문제: 회원 탈퇴 후 같은 번호로 재가입 시 phone_hash UNIQUE 위반
- 해결: **탈퇴 시 phone_hash를 NULL로 변경** (MySQL UNIQUE는 NULL 중복 허용)
- 원본 개인정보는 `withdrawn_member_logs` 테이블에 보관
  - `phone_hash_original`, `name_original`, `birth_encrypted_original`
  - 30일 후 익명화 스케줄러가 위 컬럼을 NULL 처리
  - 개인정보보호법 준수: 탈퇴 후 30일 이내 파기
- Member.withdraw() 호출 시: status → WITHDRAWN, phone_hash → NULL, phone_encrypted → NULL, softDelete()

**예약 중복 방지**
- DB UNIQUE 제약은 걸지 않음 (취소 후 재예약 허용)
- 비즈니스 로직에서 CONFIRMED/WAITING 상태만 중복 체크

---

## 3. 동시성 처리 전략

### 정기권 잔여 횟수 차감 — 비관적 락
```sql
SELECT * FROM memberships WHERE id = ? FOR UPDATE
```
- `@Lock(LockModeType.PESSIMISTIC_WRITE)` 사용
- 이유: 잔여 횟수 차감은 **절대로 이중 차감되면 안 되는** 금전적 영향이 있는 연산
- 트레이드오프: 동시 요청 시 직렬화되어 대기 발생, 그러나 소규모 스튜디오(50명 동시)에서는 문제 없음

### 수업 정원 관리 — 낙관적 락 + UNIQUE 제약
- `class_schedules.current_count` + `@Version` 컬럼
- 예약 생성 시: `current_count < max_capacity` 확인 후 +1, 버전 충돌 시 재시도
- 추가 방어: `reservations` 테이블에 `(member_id, class_schedule_id)` 기반 중복 예약 방지 (비즈니스 로직에서 CONFIRMED/WAITING 상태만 체크)
- `current_count`를 비정규화한 이유: 매 예약마다 `COUNT(*)` 쿼리를 치면 정원 8명 그룹에서도 동시 요청 시 race condition 발생

### 결제 중복 방지 — UNIQUE 제약
- `payments.order_id` UNIQUE
- 클라이언트에서 생성한 고유 주문번호를 서버에서 INSERT 시도, 중복이면 409 응답

---

## 4. 개인정보 보호

### 휴대폰 번호
- `phone_encrypted`: AES-256 암호화 원본 (복호화하여 표시용)
- `phone_hash`: SHA-256 해시 (UNIQUE, 검색용)
- 검색 흐름: 입력된 번호를 SHA-256 → `phone_hash`로 조회 → 매칭된 레코드의 `phone_encrypted`를 복호화

### 이름, 생년월일
- `name`: AES-256 암호화 저장
- `birth_encrypted`: AES-256 암호화 저장
- 이유: 개인정보보호법 준수, 평문 저장 금지

### 비밀번호
- `admins.password_hash`: BCrypt 해시 저장
- 복호화 불가, 비교만 가능

---

## 5. 수업 유형 및 정기권 설계

### 수업 유형 (lesson_types) — 동적 관리
- DB 테이블로 관리, 관리자가 추가/수정 가능
- 기본 시드: 개인(정원1, 50분), 듀엣(정원2, 50분), 그룹(정원8, 50분), 체험(정원1, 50분)
- 정원(`max_capacity`)은 수업 유형 레벨에서 기본값, `class_schedules`에서 수업별 오버라이드 가능

### 정기권 (memberships) — 수업 유형 매핑 유연화 (결정: 2026-05-08)
- **기존**: `memberships.lesson_type_id`로 1:1 매핑 → 겸용 정기권 불가
- **변경**: `membership_lesson_types` 중간 테이블로 1:N 매핑
  - 1개 정기권이 여러 수업 유형에서 사용 가능 (예: 개인+듀엣 겸용)
  - 단순 1:1도 이 구조로 처리 (매핑 1건만 생성)
  - `UNIQUE(membership_id, lesson_type_id)` 제약으로 중복 매핑 방지
- `total_count` / `remaining_count`로 횟수 관리
- 유효기간: `start_date` ~ `end_date` (발급 시 관리자가 직접 지정)
- 무제한권: `is_unlimited = true`, `remaining_count` 사용하지 않음
  - 월 한도는 `studio_settings`의 `UNLIMITED_MONTHLY_LIMIT=30`으로 관리
  - `reservations` 테이블에서 `(member_id, status, created_at)` 복합 인덱스로 COUNT 쿼리 성능 확보

### 개인 레슨 2회 차감
- `lesson_types` 테이블에 `deduction_count INT DEFAULT 1` 컬럼 추가
- 개인 레슨은 `deduction_count = 2`, 일반은 1
- 예약 시 `membership.remaining_count -= lesson_type.deduction_count`

---

## 6. 스케줄 구조

### 2단계 구조: 고정 스케줄 → 수업 시간표

1. **fixed_schedules**: 주간 반복 템플릿 (예: "매주 월요일 10:00 김강사 개인")
2. **class_schedules**: 실제 날짜별 수업 인스턴스 (예: "2026-05-12 월 10:00")

- 매주 배치/수동으로 `fixed_schedules` → `class_schedules` 생성
- 단건 수업: `fixed_schedule_id = NULL`로 직접 생성
- 수업 취소: `class_schedules.status = CANCELLED`, 해당 예약 건 일괄 취소 + 알림

### 노쇼 자동 마킹
- `class_schedules.end_time` + `class_schedules.class_date`로 수업 종료 시각 계산
- 스케줄러가 수업 종료 후 일정 시간 뒤 미출석 예약을 NO_SHOW로 자동 변경

---

## 7. 인덱스 전략

### 원칙
- 모든 FK 컬럼에 인덱스 (MySQL InnoDB는 FK에 자동 생성하지만 명시)
- 자주 조회되는 검색 조건에 복합 인덱스
- 쓰기 성능을 고려하여 과도한 인덱스 지양

### 주요 인덱스

| 테이블 | 인덱스 | 용도 |
|--------|--------|------|
| `members` | `phone_hash` (UNIQUE) | 휴대폰 검색/중복 확인 |
| `members` | `status, deleted_at` | 활성 회원 목록 조회 |
| `memberships` | `member_id, status` | 회원별 활성 정기권 조회 |
| `memberships` | `end_date` | 만료 임박 정기권 배치 조회 |
| `class_schedules` | `class_date, instructor_id` | 주간 캘린더 (강사별 일별) |
| `class_schedules` | `class_date, status` | 일별 수업 현황 |
| `reservations` | `class_schedule_id, status` | 수업별 예약 인원 카운트 |
| `reservations` | `member_id, status` | 회원별 예약 목록 |
| `payments` | `order_id` (UNIQUE) | 중복 결제 방지 |
| `payments` | `paid_at` | 매출 통계 범위 검색 |
| `attendances` | `reservation_id` (UNIQUE) | 예약당 출석 1건 보장 |
| `notifications` | `member_id, status` | 회원별 알림 조회 |
| `notifications` | `scheduled_at, status` | 발송 대기 알림 배치 조회 |

---

## 8. 데이터 타입 결정

| 항목 | 타입 | 이유 |
|------|------|------|
| 금액 | `DECIMAL(10,0)` | 원화는 소수점 없음, FLOAT 사용 금지 |
| 시간 | `TIMESTAMP` | UTC 저장, 앱에서 KST 변환 |
| 불리언 | `TINYINT(1)` | MySQL BOOLEAN의 실제 타입 |
| Enum | `VARCHAR` + 코드 | DB ENUM은 변경이 어려워 비추, JPA `@Enumerated(STRING)` |
| 메모 | `TEXT` | 길이 제한 없는 자유 입력 |
| 암호화 필드 | `VARCHAR(512)` | AES-256 암호문은 원본보다 길어짐 |
| 해시 필드 | `VARCHAR(64)` | SHA-256 hex 출력 고정 64자 |

---

## 9. FK ON DELETE 정책

| FK | 정책 | 이유 |
|----|------|------|
| 대부분의 FK | `RESTRICT` | soft delete 사용하므로 물리 삭제 방지 |
| `member_memos.member_id` | `CASCADE` | 회원 물리 삭제 시 메모도 삭제 (실제로는 soft delete라 호출 안 됨) |
| `instructor_available_times.instructor_id` | `CASCADE` | 강사 삭제 시 가용시간도 삭제 |
| `membership_holdings.membership_id` | `CASCADE` | 정기권 삭제 시 홀딩 이력도 삭제 |
| `membership_lesson_types.membership_id` | `CASCADE` | 정기권 삭제 시 매핑도 삭제 |

---

## 10. Flyway 마이그레이션 분리 전략 (결정: 2026-05-08)

### 디렉토리 구조
```
db/
├── migration/              # 모든 환경 공통
│   ├── V1__init_schema.sql  # 테이블 생성 (19개)
│   └── V2__add_indexes.sql  # 인덱스 추가 (30개)
├── seed/
│   ├── dev/
│   │   └── R__dev_seed.sql  # 개발 시드 (Flyway repeatable, 매 변경 시 재적용)
│   └── prod/
│       ├── prod_init.sql    # 운영 초기 세팅 (수동 1회 실행)
│       └── README.md        # 운영 실행 가이드
```

### 프로파일별 flyway.locations
| 프로파일 | locations | 시드 포함 |
|----------|-----------|-----------|
| `local` | `classpath:db/migration, classpath:db/seed/dev` | ✅ 개발 시드 |
| `local-h2` | `classpath:db/migration, classpath:db/seed/dev` | ✅ 개발 시드 |
| `prod` | `classpath:db/migration` | ❌ 시드 없음 |
| `test` | Flyway 비활성화 (JPA ddl-auto=create-drop) | ❌ |

### 운영 사고 방지
- V3 시드 파일을 `db/migration/`에서 제거 → 운영에서 시드 데이터 실행 불가
- 운영 초기 세팅은 `prod_init.sql`을 DBA가 수동 실행
- 관리자 비밀번호는 스크립트에 하드코딩하지 않고, 배포 전 BCrypt 해시 교체

---

## 11. 회색 지대 기록 (STEP 4 전에 결정)

| # | 항목 | 현재 상태 | 결정 시점 |
|---|------|-----------|-----------|
| 1 | 암호화 키 관리 (AES 키 보관) | placeholder | 도메인 구현 시 |
| 3 | 수업 자동 생성 트리거 (배치 방식) | placeholder | 도메인 구현 시 |
| 4 | 노쇼 자동 처리 + 정기권 차감 시점 | placeholder | 도메인 구현 시 |
| 7 | 결제 PG 연동 범위 | payments.order_id로 자체/PG 모두 대응 가능 | SPEC 재확인 후 |
