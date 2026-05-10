# STEP 4: 회원 도메인 구현

## 작업 일시
2026-05-09 ~ 2026-05-12

## 작업 요약
회원(Member) 도메인의 전체 lifecycle을 구현.
공통 보안 인프라 → SMS 인증 → 회원가입/로그인 → 정보 관리 → 탈퇴 → 프로필 사진 → 통합 테스트 + 보안 검수.

## Phase 구성

| Phase | 내용 | 상태 |
|-------|------|------|
| 1 | 공통 보안 인프라 (AES, SHA-256, JWT, Security, @LoginMember) | 완료 |
| 2 | SMS 인증 시스템 (SmsService, MockSms, Redis 인증번호) | 완료 |
| 3 | 회원가입/로그인 플로우 (SMS 인증 → 가입 → 로그인 → 토큰 갱신) | 완료 |
| 4 | 회원 정보 관리 (내 정보 조회/수정, 비밀번호 재설정) | 완료 |
| 5 | 회원 탈퇴 (withdrawn_member_log 기록, phone_hash NULL) | 완료 |
| 6 | 프로필 사진 업로드 (로컬 임시 저장, R2는 나중에) | 완료 |
| 7 | 통합 테스트 + 보안 검수 + 문서화 | 완료 |

## 구현 API 목록

### 인증 (Auth) — /api/auth/** (permitAll)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /api/auth/sms/request | SMS 인증번호 발송 (1분 1회, 일 5회 제한) |
| POST | /api/auth/sms/verify | SMS 인증번호 검증 → verifiedToken 발급 |
| POST | /api/auth/signup | 회원가입 (verifiedToken 필요) |
| POST | /api/auth/login | 로그인 (Access + Refresh 토큰) |
| POST | /api/auth/refresh | 토큰 갱신 (Refresh Rotation) |
| POST | /api/auth/reset-password | 비밀번호 재설정 (verifiedToken 필요) |

### 회원 (Member) — /api/members/** (인증 필요)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/members/me | 내 정보 조회 (복호화) |
| PATCH | /api/members/me | 내 정보 수정 (이름, 생년월일) |
| DELETE | /api/members/me | 회원 탈퇴 (soft delete) |
| POST | /api/members/me/profile-image | 프로필 사진 업로드 |
| DELETE | /api/members/me/profile-image | 프로필 사진 삭제 |

## 보안 검수 결과 (15항목)

| # | 항목 | 상태 |
|---|------|------|
| 1 | verifiedToken 1회용 | 정상 |
| 2 | 동시 가입 동시성 (DataIntegrityViolation) | 정상 |
| 3 | 마지막 로그인 시각 | 미구현 (v2) |
| 4 | 비밀번호 재설정 토큰 재사용 차단 | 정상 |
| 5 | PATCH 동시성 (last-write-wins) | 정상 (주석 명시) |
| 6 | 탈퇴 시 예약/정기권 TODO | 정상 (주석) |
| 7 | 익명화 스케줄러 + 테스트 | 정상 |
| 8 | TestAuthController prod 비활성 | 정상 (@Profile) |
| 9 | 매직 넘버 이미지 검증 | 정상 |
| 10 | 이미지 리사이즈 | 미구현 (R2 연동 시) |
| 11 | 파일명 안전 처리 | 정상 |
| 12 | E2E 통합 테스트 4종 | 정상 (전체 통과) |
| 13 | Swagger 문서화 | 정상 (11 엔드포인트) |
| 14 | ARCHITECTURE.md | 정상 (191줄) |
| 15 | backend/README.md | 정상 |

## Flyway 마이그레이션
| 버전 | 파일 | 내용 |
|------|------|------|
| V3 | add_member_profile_image.sql | profile_image_url, profile_image_uploaded_at |
| V4 | add_member_password.sql | password_hash |
| V5 | fix_withdrawn_member_log_columns.sql | name_original, birth_encrypted_original VARCHAR 512 |

## 커밋 이력
| 해시 | 메시지 |
|------|--------|
| 2714dfa | feat(security): AES 암호화 + SHA-256 해시 + 비밀번호 정책 |
| 82bcb39 | feat(security): JWT 토큰 발급/검증 + Spring Security 통합 |
| 5560f67 | feat(common): ErrorCode 확장 + @LoginMember 검증 엔드포인트 |
| 43746c7 | test: 보안 컴포넌트 테스트 (암호화, 해시, JWT, 비밀번호) |
| 6d1a6a1 | fix(security): JWT 에러 시 구체적 에러 코드 (AUTH_001~003) |
| af78a02 | feat(auth): SMS 인증 시스템 (Phase 2) |
| 317ce91 | fix(auth): Redis 장애 시 SMS_006 에러 응답 (503) |
| 87fd3ca | feat(auth): 회원가입/로그인/토큰 갱신 (Phase 3) |
| afa96c5 | feat(member): 회원 정보 관리 + 탈퇴 + 프로필 사진 (Phase 4-6) |
| 7aac226 | fix(security): 보안 검수 보강 (동시성, 매직넘버, 스케줄러) |
| 3e016a4 | refactor(member): PATCH 동시성 정책 주석 |
| 0024566 | test(member): 익명화 스케줄러 단위 테스트 |
| 7230c87 | test(integration): member 도메인 E2E 시나리오 4종 |
| 6022e29 | docs(api): Swagger 문서화 (Auth, Member) |
| eda27d5 | docs(architecture): member 도메인 아키텍처 + 시퀀스 다이어그램 |

## 미완료 (다음 단계)
- last_login_at 컬럼 + 갱신 (v2)
- 이미지 리사이즈 200x200, 500x500 (R2 연동 시)
- 탈퇴 시 미래 예약 취소 + 정기권 복구 (reservation 도메인 후)
