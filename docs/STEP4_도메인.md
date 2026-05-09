# STEP 4: 회원 도메인 구현

## 작업 일시
2026-05-12 ~

## 작업 요약
회원(Member) 도메인의 전체 lifecycle을 구현한다.
공통 보안 인프라 → SMS 인증 → 회원가입/로그인 → 정보 관리 → 탈퇴 → 프로필 사진 → 통합 테스트.

## Phase 구성

| Phase | 내용 | 상태 |
|-------|------|------|
| 1 | 공통 보안 인프라 (AES, SHA-256, JWT, Security, @LoginMember) | 진행 중 |
| 2 | SMS 인증 시스템 (SmsService, MockSms, Redis 인증번호) | 대기 |
| 3 | 회원가입/로그인 플로우 (SMS 인증 → 가입 → 로그인 → 토큰 갱신) | 대기 |
| 4 | 회원 정보 관리 (내 정보 조회/수정, 비밀번호 재설정) | 대기 |
| 5 | 회원 탈퇴 (withdrawn_member_log 기록, phone_hash NULL) | 대기 |
| 6 | 프로필 사진 업로드 (로컬 임시 저장, R2는 나중에) | 대기 |
| 7 | 통합 테스트 (전체 시나리오 E2E) | 대기 |

---

## Phase 1: 공통 보안 인프라

### 작업 항목
1. 암호화 컴포넌트 — AES-256/GCM, IV 랜덤, 버전 prefix "v1::"
2. 해시 컴포넌트 — SHA-256 hex, 전화번호 정규화
3. JWT — jjwt 0.12.x, access(30분)/refresh(14일), HS256
4. Spring Security — JWT 필터, EntryPoint, AccessDenied, STATELESS
5. @LoginMember — ArgumentResolver로 현재 로그인 사용자 주입
6. 비밀번호 — BCrypt strength 12, PasswordPolicy
7. ErrorCode 확장 — AUTH, ENCRYPTION 관련

### 패키지 구조
```
com.pilates.common.security/
├── encryption/
│   ├── EncryptionService.java
│   └── EncryptionKeyProperties.java
├── hash/
│   ├── HashingService.java
│   └── PhoneNumberNormalizer.java
├── jwt/
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationException.java
├── password/
│   └── PasswordPolicy.java
└── auth/
    ├── LoginMember.java (record)
    ├── LoginMemberAnnotation.java (@LoginMember)
    └── LoginMemberArgumentResolver.java

com.pilates.config.security/
├── SecurityConfig.java (갱신)
├── JwtAuthenticationFilter.java
├── JwtAuthenticationEntryPoint.java
└── CustomAccessDeniedHandler.java
```
