# 회원 도메인 아키텍처

## 1. 패키지 구조

```
com.pilates/
├── common/
│   ├── controller/     — 공통 컨트롤러 (Health, Test)
│   ├── entity/         — BaseEntity (audit + soft delete)
│   ├── error/          — ErrorCode, BusinessException, GlobalExceptionHandler
│   ├── logging/        — LoggingFilter (요청/응답 로깅, PII 마스킹)
│   ├── response/       — ApiResponse, PageResponse 래퍼
│   ├── security/
│   │   ├── auth/       — LoginMember, @LoginMemberAnnotation, ArgumentResolver
│   │   ├── encryption/ — AES-256/GCM 암호화 (EncryptionService)
│   │   ├── hash/       — SHA-256 해시, 전화번호 정규화
│   │   ├── jwt/        — JWT 발급/검증 (JwtTokenProvider)
│   │   └── password/   — 비밀번호 정책 (PasswordPolicy)
│   ├── notification/
│   │   └── kakao/      — KakaoAlimtalkClient, Mock/Real 구현체
│   ├── sms/            — SmsService 인터페이스, MockSmsService
│   └── util/           — MaskingUtil
├── config/
│   ├── security/       — JwtFilter, EntryPoint, AccessDeniedHandler
│   ├── SecurityConfig  — Spring Security 설정
│   ├── WebConfig       — CORS, ArgumentResolver 등록
│   └── ...             — JPA, Redis, QueryDSL, Swagger 설정
└── domain/
    ├── auth/
    │   ├── controller/  — SmsController, AuthController
    │   ├── dto/         — 요청/응답 DTO (record)
    │   └── service/     — SmsVerificationService, AuthService
    ├── member/
    │   ├── controller/  — MemberController
    │   ├── dto/         — MemberResponse, MemberUpdateRequest 등
    │   ├── entity/      — Member, Gender, MemberStatus, WithdrawnMemberLog, MemberMemo
    │   ├── repository/  — MemberRepository, WithdrawnMemberLogRepository
    │   └── service/     — MemberService, ProfileImageService, AnonymizationScheduler
    └── (instructor, classroom, membership, reservation, payment, notification, admin)
```

## 2. 회원 도메인 흐름

### 2.1 회원가입 시퀀스

```mermaid
sequenceDiagram
    participant C as Client
    participant SC as SmsController
    participant SVS as SmsVerificationService
    participant SMS as MockSmsService
    participant R as Redis
    participant AC as AuthController
    participant AS as AuthService
    participant ES as EncryptionService
    participant HS as HashingService
    participant DB as MySQL
    participant JWT as JwtTokenProvider

    Note over C: 1단계: SMS 인증
    C->>SC: POST /api/auth/sms/request {phoneNumber}
    SC->>SVS: sendVerificationCode(normalized)
    SVS->>R: SET sms:rate:{phone} (60초 TTL)
    SVS->>R: SET sms:code:{phone} = 6자리코드 (5분 TTL)
    SVS->>R: INCR sms:daily:{phone} (24시간 TTL)
    SVS->>SMS: send(phone, "인증번호: XXXXXX")
    SC-->>C: 200 OK

    C->>SC: POST /api/auth/sms/verify {phoneNumber, code}
    SC->>SVS: verifyCode(normalized, code)
    SVS->>R: GET sms:code:{phone}
    SVS->>R: DEL sms:code:{phone}
    SVS->>R: SET sms:verified:{token} = phone (10분 TTL)
    SC-->>C: 200 {verifiedToken}

    Note over C: 2단계: 회원가입
    C->>AC: POST /api/auth/signup {verifiedToken, name, password, gender}
    AC->>AS: signup(request)
    AS->>SVS: getVerifiedPhoneNumber(token) → phone (1회용, 즉시 삭제)
    AS->>AS: passwordPolicy.validate(password)
    AS->>HS: hash(phone) → phoneHash
    AS->>DB: existsByPhoneHash(phoneHash) → 중복 검사
    AS->>ES: encrypt(name), encrypt(phone), encrypt(birth)
    AS->>AS: passwordEncoder.encode(password) → BCrypt
    AS->>DB: INSERT member
    AS->>JWT: createAccessToken(id, MEMBER) → accessToken
    AS->>JWT: createRefreshToken(id) → refreshToken (jti 포함)
    AS->>R: SET auth:refresh:{id} = refreshToken (14일 TTL)
    AC-->>C: 200 {publicId, accessToken, refreshToken, expiresIn}
```

### 2.2 로그인 시퀀스

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant AS as AuthService
    participant PN as PhoneNumberNormalizer
    participant HS as HashingService
    participant DB as MySQL
    participant PE as PasswordEncoder
    participant JWT as JwtTokenProvider
    participant R as Redis

    C->>AC: POST /api/auth/login {phoneNumber, password}
    AC->>AS: login(request)
    AS->>PN: normalize(phoneNumber) → 01012345678
    AS->>HS: hash(normalized) → phoneHash (64자 hex)
    AS->>DB: findByPhoneHash(phoneHash)
    alt 회원 없음 또는 탈퇴
        AS-->>C: 401 AUTH_010 "번호 또는 비밀번호 오류"
    end
    AS->>PE: matches(password, member.passwordHash)
    alt 비밀번호 불일치
        AS-->>C: 401 AUTH_010 (동일 에러 — 정보 노출 방지)
    end
    AS->>JWT: createAccessToken(id, MEMBER)
    AS->>JWT: createRefreshToken(id)
    AS->>R: SET auth:refresh:{id} = refreshToken (이전 토큰 자동 교체)
    AC-->>C: 200 {accessToken, refreshToken, expiresIn}
```

### 2.3 토큰 갱신 시퀀스 (Refresh Rotation)

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant AS as AuthService
    participant JWT as JwtTokenProvider
    participant R as Redis

    C->>AC: POST /api/auth/refresh {refreshToken}
    AC->>AS: refresh(request)
    AS->>JWT: validateToken(refreshToken) → 서명/만료 검증
    AS->>JWT: getTokenType() → "refresh" 확인
    AS->>JWT: getMemberIdFromToken() → memberId
    AS->>R: GET auth:refresh:{memberId}
    alt Redis 토큰 != 요청 토큰 (재사용 감지)
        AS->>R: DEL auth:refresh:{memberId} (모든 세션 무효화)
        AS-->>C: 401 AUTH_002 "유효하지 않은 토큰"
    end
    AS->>JWT: createAccessToken(memberId, MEMBER) → newAccess
    AS->>JWT: createRefreshToken(memberId) → newRefresh (새 jti)
    AS->>R: SET auth:refresh:{memberId} = newRefresh (이전 것 교체)
    AC-->>C: 200 {newAccessToken, newRefreshToken, expiresIn}
```

### 2.4 탈퇴 시퀀스

```mermaid
sequenceDiagram
    participant C as Client
    participant MC as MemberController
    participant MS as MemberService
    participant DB as MySQL

    C->>MC: DELETE /api/members/me?reason=... (Bearer accessToken)
    MC->>MS: withdraw(memberId, reason)
    Note over MS: TODO: 미래 예약 자동 취소 + 정기권 복구
    MS->>DB: INSERT withdrawn_member_logs (phone_hash, name, birth 원본 보관)
    MS->>DB: UPDATE member: status=WITHDRAWN, phone_hash=NULL, phone_encrypted=NULL, deleted_at=NOW
    MC-->>C: 200 OK

    Note over DB: 30일 후 (매일 3시 스케줄러)
    DB->>DB: withdrawn_member_logs: phone_hash_original=NULL, name_original=NULL, anonymized=true
```

## 3. 보안 정책

| 항목 | 방식 | 상세 |
|------|------|------|
| 이름·생년월일 | AES-256/GCM | 랜덤 IV, 키 버전 prefix `v1::` |
| 휴대폰 (검색) | SHA-256 | 64자 hex, 정규화 후 해시 |
| 휴대폰 (표시) | AES-256/GCM | 복호화하여 표시, 탈퇴 시 NULL |
| 비밀번호 | BCrypt | strength 12, 평문 로그 노출 방지 |
| JWT Access | HS256 | 30분, subject=memberId, role claim |
| JWT Refresh | HS256 + jti | 14일, Redis whitelist, Rotation |
| SMS Rate Limit | Redis TTL | 1분 1회, 일 5회 (고정 TTL) |
| 이미지 검증 | 매직 넘버 | JPEG(FF D8), PNG(89 50 4E 47), WebP(RIFF+WEBP) |
| 로그 PII | 마스킹 | password, code, verifiedToken 항상 `***` 처리 |

## 4. 데이터 정책

| 정책 | 대상 | 상세 |
|------|------|------|
| Soft Delete | members, instructors, memberships, reservations, admins | `deleted_at` TIMESTAMP NULL |
| 탈퇴 시 phone_hash NULL | members | 같은 번호 재가입 허용 (UNIQUE 우회) |
| 30일 익명화 | withdrawn_member_logs | 스케줄러가 매일 3시 실행 |
| 동시성 (정기권) | memberships.remaining_count | 비관적 락 `FOR UPDATE` |
| 동시성 (수업 정원) | class_schedules.current_count | 낙관적 락 `@Version` |
| 동시성 (회원 정보) | members | last-write-wins (v1) |

## 5. 강사 + 수업 도메인

### 5.1 패키지 구조
```
domain/instructor/
├── controller/  AdminInstructorController, PublicInstructorController
├── dto/         InstructorRegisterRequest, InstructorResponse, AvailableTimeRequest 등
├── repository/  InstructorRepository, InstructorAvailableTimeRepository
└── service/     InstructorService

domain/classroom/
├── controller/  AdminLessonTypeController, AdminFixedScheduleController,
│                AdminClassScheduleController, MemberClassScheduleController,
│                PublicLessonTypeController
├── dto/         LessonTypeRequest, FixedScheduleRequest, ClassScheduleResponse 등
├── repository/  LessonTypeRepository, FixedScheduleRepository, ClassScheduleRepository
├── scheduler/   ClassScheduleGenerator (매주 일요일 자정)
└── service/     LessonTypeService, FixedScheduleService, ClassScheduleService
```

### 5.2 시간표 구조 (2단계)

```mermaid
graph LR
    FS[FixedSchedule<br>주간 반복 템플릿] -->|매주 자동 생성| CS[ClassSchedule<br>실제 수업 인스턴스]
    AD[관리자 단건 등록] --> CS
    CS -->|예약| R[Reservation<br>STEP 8]
```

- **FixedSchedule**: "매주 월요일 10:00 박지영 그룹" (템플릿)
- **ClassSchedule**: "2026-05-12 월 10:00 박지영 그룹" (실제 수업)
- 자동 생성: 매주 일요일 자정, 다음 4주치
- 수동 생성: 관리자 API로 즉시 (멱등)

### 5.3 자동 생성 시퀀스

```mermaid
sequenceDiagram
    participant S as Scheduler (매주 일)
    participant FS as FixedScheduleRepo
    participant H as HolidayRepo
    participant CS as ClassScheduleRepo

    S->>FS: 활성 고정 스케줄 전체 조회
    loop 각 FixedSchedule
        loop 대상 기간의 매칭 요일
            S->>H: 공휴일인가?
            alt 공휴일
                S->>S: skip
            end
            S->>CS: 이미 존재하는가?
            alt 이미 존재
                S->>S: skip
            end
            S->>CS: INSERT ClassSchedule
        end
    end
    S->>S: log("생성 N건, 스킵 M건")
```

### 5.4 권한별 API 매트릭스

| API 경로 | ADMIN | INSTRUCTOR | MEMBER | 비인증 |
|----------|-------|------------|--------|--------|
| /api/admin/instructors/** | O | X | X | X |
| /api/admin/lesson-types/** | O | X | X | X |
| /api/admin/fixed-schedules/** | O | X | X | X |
| /api/admin/class-schedules/** | O | X | X | X |
| /api/instructors/** | O | O | O | O* |
| /api/lesson-types/** | O | O | O | O* |
| /api/class-schedules/** | O | O | O | O* |

\* v1에서는 공개 조회 API는 permitAll (인증 불필요)

## 6. 정기권(Membership) 도메인

### 6.1 상태 머신

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: 발급
    ACTIVE --> HOLDING: 일시정지
    ACTIVE --> EXHAUSTED: 잔여 0
    ACTIVE --> EXPIRED: 만료 (스케줄러)
    HOLDING --> ACTIVE: 해제 (endDate += 일시정지 일수)
    EXHAUSTED --> ACTIVE: 복구 (예약 취소 시)
```

### 6.2 발급 시퀀스

```mermaid
sequenceDiagram
    participant A as Admin
    participant C as AdminMembershipController
    participant S as MembershipService
    participant DB as MySQL

    A->>C: POST /api/admin/memberships {memberId, totalCount, price, validityDays, lessonTypeIds}
    C->>S: issueMembership(request)
    S->>DB: Member 존재 확인
    S->>DB: LessonType 존재 확인
    S->>DB: INSERT membership (publicId=UUID, start=today, end=today+days, status=ACTIVE)
    S->>DB: INSERT membership_lesson_types (매핑)
    S-->>A: 200 {membership 상세}
```

### 6.3 차감/복구 정책
- **차감**: `Membership.deduct(count)` — 비관적 락 하에서 호출 (STEP 8 reservation)
- **무제한권**: 차감 없음, 월 한도는 Service 레벨 카운팅
- **복구**: `Membership.restore(count)` — 예약 취소 시 (STEP 8)
- **소진**: 잔여 0이면 자동 EXHAUSTED, 복구 시 ACTIVE 복귀

### 6.4 만료 스케줄러
- 매일 새벽 4시 실행
- end_date < today AND status NOT IN (EXPIRED, EXHAUSTED, HOLDING)
- HOLDING 중 만료: HOLDING 우선 유지 (TODO: 의뢰인 정책 확인)

\* v1에서는 공개 조회 API는 permitAll (인증 불필요)

## 7. 인증·권한 시스템

### 7.1 역할 (Role)

| 역할 | JWT subject | JWT claim | 설명 |
|------|------------|-----------|------|
| MEMBER | members.id | role=MEMBER | 회원 (SMS 가입) |
| INSTRUCTOR | admins.id | role=INSTRUCTOR, instructorId | 강사 (admin 로그인) |
| ADMIN | admins.id | role=ADMIN | 관리자 |
| SUPER_ADMIN | admins.id | role=SUPER_ADMIN | 슈퍼 관리자 |

### 7.2 권한 매트릭스

| 경로 | MEMBER | INSTRUCTOR | ADMIN | SUPER_ADMIN |
|------|--------|------------|-------|-------------|
| /api/auth/** | permitAll | | | |
| /api/admin/auth/** | permitAll | | | |
| /api/health, /api/test/** | permitAll | | | |
| /api/instructors/** (공개) | permitAll | | | |
| /api/class-schedules/** (공개) | permitAll | | | |
| /api/admin/** | X | O | O | O |
| /api/instructor/** | X | O | O | O |
| /api/members/me/** | O | O | O | O |
| 기타 (인증 필요) | O | O | O | O |

### 7.3 인증 시퀀스

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AdminAuthController
    participant AS as AdminAuthService
    participant DB as MySQL
    participant R as Redis

    Note over C,R: 강사·관리자 로그인
    C->>AC: POST /api/admin/auth/login {loginId, password}
    AC->>AS: login(request)
    AS->>DB: Admin 조회 (login_id)
    AS->>AS: BCrypt 비밀번호 검증
    AS->>AS: JWT 발급 (subject=adminId, role, instructorId)
    AS->>R: Refresh Token 저장
    AS-->>C: 200 {adminId, role, instructorId, tokens}
```

### 7.4 JWT 토큰 구조
- **subject**: userId (members.id 또는 admins.id)
- **claim "role"**: MEMBER / INSTRUCTOR / ADMIN / SUPER_ADMIN
- **claim "instructorId"**: 강사인 경우만 (instructors.id)
- **claim "type"**: access / refresh

## 9. attendance 도메인

### 9.1 패키지 구조

```
domain/attendance/
├── controller/
│   ├── InstructorAttendanceController  — 강사 출석 체크 API
│   ├── MemberAttendanceController      — 회원 본인 이력 API
│   └── AdminAttendanceController       — 관리자 통계 API
├── dto/
│   ├── AttendanceMarkRequest           — 단건 출석 요청
│   ├── BatchAttendanceRequest          — 일괄 출석 요청
│   ├── AttendanceResponse              — 출석 응답
│   ├── AttendanceRateResponse          — 출석률 응답
│   └── NoShowCountResponse             — 노쇼 카운트 응답
├── entity/
│   ├── Attendance                      — 출석 엔티티 (reservation 1:1)
│   └── AttendanceStatus                — PENDING → ATTENDED/LATE/ABSENT/NO_SHOW
├── repository/
│   └── AttendanceRepository            — JPA 쿼리
└── service/
    ├── InstructorAttendanceService     — 강사 출석 체크 로직
    └── MemberAttendanceService         — 이력/출석률 조회
```

### 9.2 출석 상태 머신

```
                 ┌─── markAttended(instructorId) ──→ ATTENDED
                 │
PENDING ─────────┼─── markLate(instructorId) ──────→ LATE
  (예약 생성 시)  │
                 ├─── markAbsent(instructorId) ────→ ABSENT
                 │
                 └─── markNoShow() (스케줄러) ─────→ NO_SHOW

* 강사 마킹 간 재변경 허용 (ATTENDED ↔ LATE ↔ ABSENT)
* 강사가 마킹한 건(ATTENDED/LATE/ABSENT)은 노쇼 스케줄러가 덮어쓰지 않음
```

### 9.3 출석 체크 시퀀스

```mermaid
sequenceDiagram
    participant I as Instructor
    participant C as InstructorAttendanceController
    participant S as InstructorAttendanceService
    participant DB as MySQL

    I->>C: POST /api/instructor/attendances/{reservationId} {status: ATTENDED}
    C->>S: markAttendance(instructorId, reservationId, status)
    S->>DB: Attendance 조회 (by reservationId)
    S->>S: 본인 수업 검증 (instructor.id == instructorId)
    S->>S: isCheckable 검증 (수업시작 ~ 종료+30분)
    S->>DB: status 갱신 + checkedAt + checkedBy
    S-->>I: 200 OK
```

### 9.4 노쇼 자동 마킹 흐름

```mermaid
sequenceDiagram
    participant SCH as NoShowMarkingScheduler
    participant RR as ReservationRepository
    participant AR as AttendanceRepository
    participant DB as MySQL

    SCH->>RR: 종료+30분 경과 CONFIRMED 예약 조회
    loop 각 예약
        SCH->>DB: reservation.status → NO_SHOW
        SCH->>AR: Attendance 조회 (by reservationId)
        alt PENDING 상태 또는 미존재
            SCH->>DB: attendance.status → NO_SHOW
        else 강사 마킹 완료 (ATTENDED/LATE/ABSENT)
            Note over SCH: 건드리지 않음
        end
    end
```

### 9.5 예약-출석 연동
- **예약 생성**: `Attendance.createPending(reservation)` 자동 INSERT
- **예약 취소**: `attendanceRepository.deleteByReservationId()` 삭제
- **휴강 취소**: 각 예약의 Attendance도 삭제

### 9.6 API 엔드포인트

| Method | Path | Role | 설명 |
|--------|------|------|------|
| POST | /api/instructor/attendances/{reservationId} | INSTRUCTOR | 단건 출석 체크 |
| POST | /api/instructor/class-schedules/{id}/attendances | INSTRUCTOR | 일괄 출석 체크 |
| GET | /api/instructor/class-schedules/{id}/attendances | INSTRUCTOR | 수업 출석 현황 |
| GET | /api/members/me/attendances | MEMBER | 내 출석 이력 (페이지네이션) |
| GET | /api/members/me/attendance-rate | MEMBER | 내 출석률 |
| GET | /api/admin/members/{id}/attendance-rate | ADMIN | 회원 출석률 |
| GET | /api/admin/attendances/no-show-counts | ADMIN | 회원별 노쇼 횟수 |

## 10. notification 도메인

### 10.1 패키지 구조

```
domain/notification/
├── controller/
│   ├── MyNotificationController     — 회원 알림 조회 API
│   └── AdminNotificationController  — 관리자 알림 관리 API
├── dto/
│   ├── NotificationResponse         — 알림 응답 DTO
│   └── NotificationStatisticsResponse — 알림 통계 DTO
├── entity/
│   ├── Notification         — 알림 발송 이력 Entity
│   ├── NotificationTemplate — 알림 템플릿 Entity
│   ├── NotificationType     — 알림 유형 Enum
│   ├── NotificationStatus   — 발송 상태 Enum (PENDING, SENT, FAILED, FALLBACK_SENT)
│   └── NotificationChannel  — 발송 채널 Enum (ALIMTALK, SMS)
├── event/
│   ├── ReservationCreatedEvent     — 예약 생성 이벤트
│   ├── ReservationCancelledEvent   — 예약 취소 이벤트
│   └── NotificationEventListener   — 이벤트 리스너 (@TransactionalEventListener)
├── repository/
│   ├── NotificationRepository
│   └── NotificationTemplateRepository
├── scheduler/
│   ├── ReservationReminderScheduler       — 수업 1시간 전 리마인드
│   └── MembershipExpirationReminderScheduler — 정기권 3일 전 만료 알림
└── service/
    ├── NotificationService       — 발송 서비스 (알림톡 → SMS 폴백)
    └── NotificationQueryService  — 조회 서비스
```

### 10.2 알림 발송 시퀀스

```mermaid
sequenceDiagram
    participant RS as ReservationService
    participant EP as EventPublisher
    participant NL as NotificationEventListener
    participant NS as NotificationService
    participant DB as Database
    participant AK as KakaoAlimtalkClient
    participant SMS as SmsService

    RS->>EP: publishEvent(ReservationCreatedEvent)
    Note over RS: 트랜잭션 커밋
    EP->>NL: @TransactionalEventListener(AFTER_COMMIT)
    NL->>NS: createNotificationForMember(memberId)
    NS->>DB: INSERT notification (PENDING, recipientType=MEMBER)
    NL->>NS: createNotificationForInstructor(instructorId)
    NS->>DB: INSERT notification (PENDING, recipientType=INSTRUCTOR)
    NL->>NS: send(notificationId) [@Async]

    alt 알림톡 성공
        NS->>AK: sendAlimtalk(phone, templateCode, params)
        AK-->>NS: success + messageId
        NS->>DB: UPDATE notification → SENT
    else 알림톡 실패 → SMS 폴백
        NS->>AK: sendAlimtalk() → 실패
        NS->>SMS: send(phone, content)
        alt SMS 성공
            NS->>DB: UPDATE notification → FALLBACK_SENT
        else SMS도 실패
            NS->>DB: UPDATE notification → FAILED + failureReason
        end
    end
```

### 10.3 스케줄러 흐름

| 스케줄러 | 주기 | 대상 | 알림 유형 |
|---------|------|------|----------|
| ReservationReminderScheduler | 10분마다 | 1시간 후 시작 수업의 CONFIRMED 예약 | REMINDER_1HOUR |
| MembershipExpirationReminderScheduler | 매일 09:00 | 3일 후 만료 ACTIVE 정기권 | MEMBERSHIP_EXPIRING |

### 10.4 권한 매트릭스 (알림)

| Method | Path | MEMBER | ADMIN |
|--------|------|--------|-------|
| GET | /api/members/me/notifications | O (본인만) | - |
| GET | /api/members/me/notifications/{id} | O (본인만) | - |
| GET | /api/admin/notifications | - | O |
| GET | /api/admin/notifications/statistics | - | O |
| POST | /api/admin/notifications/{id}/resend | - | O |

### 10.5 Recipient 일반화 패턴

notifications 테이블은 `recipient_type` + `recipient_id`로 다중 수신자를 지원한다.

| recipientType | recipientId 참조 | phone 조회 |
|---------------|-----------------|------------|
| MEMBER | members.id | encryptionService.decrypt(member.phoneEncrypted) |
| INSTRUCTOR | instructors.id | encryptionService.decrypt(instructor.phoneEncrypted) |

- `Notification.createForMember()` / `Notification.createForInstructor()` 팩토리 사용
- `NotificationService.resolvePhone()` — recipientType 분기로 phone 복호화
- 권한 검증: recipientType + recipientId 일치 확인 (회원이 강사 알림 조회 불가)

### 10.6 강사 phone 암호화 (STEP 5 보강)

V10 마이그레이션으로 instructors 테이블에 `phone_encrypted` (AES-256) + `phone_hash` (SHA-256) 추가.
회원 phone과 동일한 암호화 패턴 적용. 기존 phone 컬럼은 점진 제거 예정.

### 10.7 의뢰인 정책

- **휴강 처리 시 알림 X**: `cancelAllByClassSchedule`은 이벤트를 발행하지 않음
- **알림톡 실패 → SMS 자동 폴백**: Mock에서는 `FAIL_` prefix로 실패 시뮬레이션

---

## 11. admin 도메인

### 11.1 패키지 구조

```
domain/admin/
├── controller/
│   ├── AdminAuthController.java        # 관리자 인증 (기존)
│   ├── AdminDashboardController.java   # 대시보드 통합 조회
│   ├── AdminMemberController.java      # 회원 검색·상세·메모·강제탈퇴
│   ├── AdminStatisticsController.java  # 매출·회원·출석·인기시간 통계
│   ├── AdminBulkImportController.java  # 엑셀 일괄 등록·다운로드
│   └── AdminSettingsController.java    # 학원 설정 (SUPER_ADMIN 전용)
├── dto/
│   ├── DashboardResponse.java          # 대시보드 4영역 (수업·매출·만료·알림)
│   ├── AdminMemberResponse.java        # 회원 리스트 + 상세 (8도메인 통합)
│   ├── StatisticsResponse.java         # 4종 통계 응답
│   ├── BulkImportResponse.java         # 엑셀 일괄 결과 (부분 성공)
│   └── StudioSettingResponse.java      # 학원 설정 Key-Value
├── entity/
│   ├── Admin.java, AdminRole.java      # 기존
│   ├── AdminAuditLog.java              # 감사 로그
│   ├── StudioSetting.java              # 학원 전역 설정
│   └── Holiday.java                    # 휴무일
├── repository/
│   ├── AdminRepository.java
│   └── StudioSettingRepository.java
└── service/
    ├── AdminAuthService.java           # 기존
    ├── AdminDashboardService.java      # 대시보드 통합 쿼리
    ├── AdminMemberService.java         # 회원 관리 + 메모 CRUD
    ├── AdminStatisticsService.java     # 통계 집계
    ├── AdminBulkImportService.java     # 엑셀 파싱·등록
    └── AdminSettingsService.java       # 학원 설정 CRUD
```

### 11.2 대시보드 통합 쿼리

단일 API (`GET /api/admin/dashboard`)로 4개 영역 데이터를 한 번에 반환:

| 영역 | 데이터 소스 | 쿼리 전략 |
|------|------------|----------|
| 오늘 수업 | class_schedules | classDate = today, status ≠ CANCELLED |
| 이번 주 매출 | payments | paid_at BETWEEN 월~일, COMPLETED/REFUNDED |
| 만료 임박 정기권 | memberships | end_date ≤ today+7, ACTIVE |
| 알림 (노쇼) | attendances | 최근 30일 NO_SHOW 3회 이상 |
| 알림 (잔여 부족) | memberships | remaining_count ≤ 1, ACTIVE, !unlimited |

N+1 방지: member 정보는 ID 수집 후 `findAllById()` 일괄 조회.

### 11.3 통계 쿼리 패턴

4개 통계 엔드포인트:

- **매출**: `payments` → groupBy (daily/weekly/monthly) 집계, net = amount - refundAmount
- **회원 추이**: `members` → createdAt/deletedAt 기반 가입/탈퇴 카운트
- **출석률**: `attendances` → 강사별·수업유형별 ATTENDED+LATE / 전체(PENDING 제외) 비율
- **인기 시간대**: `class_schedules` → 시간대별·요일별 currentCount 합산

인덱스 활용: `idx_payments_paid_at_status`, `idx_members_status_created` (V12 마이그레이션).

### 11.4 엑셀 일괄 처리 흐름

```
[엑셀 업로드] → [헤더 검증] → [행별 개별 트랜잭션 처리]
                                    ├─ 성공 → INSERT (members/memberships)
                                    └─ 실패 → failures 리스트에 추가
                              → [결과: successCount + failureCount + failures[]]
```

핵심 설계:
- **부분 성공**: 각 행을 `TransactionTemplate`으로 독립 트랜잭션 처리. 50번째 실패해도 1~49번째 유지.
- **검증**: 휴대폰 형식, phone_hash 중복, 필수 필드
- **제한**: 5MB / 1000행
- **워크북 자원 관리**: try-with-resources 필수 (메모리 누수 방지)

### 11.5 권한 매트릭스

| 경로 | MEMBER | INSTRUCTOR | ADMIN | SUPER_ADMIN |
|------|--------|-----------|-------|-------------|
| /api/admin/dashboard | X | O | O | O |
| /api/admin/members/** | X | O | O | O |
| /api/admin/statistics/** | X | O | O | O |
| /api/admin/members/bulk | X | O | O | O |
| /api/admin/settings/** | X | X | X | **O (전용)** |

`SecurityConfig`에서 `/api/admin/settings/**` → `hasRole("SUPER_ADMIN")` 우선 매칭.
회원 메모 수정/삭제는 작성한 admin만 가능 (또는 SUPER_ADMIN 검증 가능).

### 11.6 V12 마이그레이션

- `member_memos`: `admin_id` 추가 (관리자도 메모 작성), `instructor_id` nullable, `deleted_at` 추가
- 통계 성능 인덱스: `idx_payments_paid_at_status`, `idx_memberships_end_date_status`, `idx_members_status_created`
