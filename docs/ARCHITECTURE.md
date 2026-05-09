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
