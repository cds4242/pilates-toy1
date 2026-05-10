# STEP 10: 알림(Notification) 도메인 구현

## 작업 일시
2026-05-09

## 작업 요약
카카오 알림톡 + SMS 폴백 + 비동기 발송 + 스케줄러 + 발송 이력.
STEP 5 회귀 보강 (강사 phone 암호화) + notifications recipient 일반화.

## 완료 상태

### 신규 생성
- **V9**: notification_templates 테이블 + notifications channel/message_id 컬럼
- **V10**: instructors phone 암호화 (phone_encrypted, phone_hash)
- **V11**: notifications recipient 일반화 (recipient_type, recipient_id)
- **알림톡 클라이언트**: KakaoAlimtalkClient 인터페이스 + Mock (FAIL_ prefix 실패 시뮬레이션) + NhnToast 운영 스텁
- **NotificationService**: 알림톡 → SMS 폴백, 비동기 (@Async notificationExecutor)
- **NotificationQueryService**: 회원/관리자 알림 조회, 통계
- **NotificationEventListener**: @TransactionalEventListener(AFTER_COMMIT)
- **스케줄러 2개**: ReservationReminderScheduler (10분), MembershipExpirationReminderScheduler (매일 9시)
- **AsyncConfig**: @EnableAsync + @EnableScheduling, notificationExecutor (core 5, max 10, queue 100)
- **InstructorPhoneMigrationRunner**: local-h2 시드 phone 자동 암호화

### 수정
- **Instructor Entity**: phone → phoneEncrypted + phoneHash (Builder, updateInfo 시그니처 변경)
- **InstructorService**: 등록/수정 시 EncryptionService + HashingService 적용
- **ReservationService**: 예약 생성/취소 시 이벤트 발행 (ApplicationEventPublisher)
- **MockSmsService**: 호출 카운트 + 강제 실패 모드 추가
- **ErrorCode**: NOTI_001~006 추가
- **SecurityConfig**: 변경 없음 (기존 규칙으로 커버)

## API 엔드포인트

### 회원 알림
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | /api/members/me/notifications | 내 알림 목록 (페이지네이션) | MEMBER |
| GET | /api/members/me/notifications/{id} | 내 알림 상세 (본인만) | MEMBER |

### 관리자 알림
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | /api/admin/notifications | 알림 목록 (필터: 회원/상태/유형/기간) | ADMIN |
| GET | /api/admin/notifications/statistics | 발송 통계 (성공률/폴백률) | ADMIN |
| POST | /api/admin/notifications/{id}/resend | 실패 알림 수동 재발송 | ADMIN |

## 알림 템플릿 5종
| 코드 | 대상 | 트리거 |
|------|------|--------|
| RESERVATION_CONFIRM | 회원 | 예약 생성 |
| RESERVATION_CANCEL | 회원 | 예약 취소 |
| REMINDER_1HOUR | 회원 | 수업 1시간 전 (스케줄러) |
| NEW_RESERVATION | 강사 | 새 예약 접수 |
| MEMBERSHIP_EXPIRING | 회원 | 정기권 3일 전 만료 (스케줄러) |

## 핵심 설계 결정

### 1. 이벤트 기반 느슨한 결합
- ReservationService → `ApplicationEventPublisher.publishEvent()`
- NotificationEventListener → `@TransactionalEventListener(AFTER_COMMIT)`
- 트랜잭션 분리: `REQUIRES_NEW`로 별도 트랜잭션

### 2. Recipient 일반화
- `recipient_type` (MEMBER / INSTRUCTOR) + `recipient_id`
- `Notification.createForMember()` / `createForInstructor()` 팩토리
- `NotificationService.resolvePhone()`: recipientType별 phone 복호화

### 3. 의뢰인 정책
- **휴강 시 알림 X**: `cancelAllByClassSchedule()`은 이벤트 미발행

### 4. 강사 phone 암호화 (STEP 5 회귀 보강)
- V10 마이그레이션: phone_encrypted (AES-256) + phone_hash (SHA-256)
- 기존 phone 컬럼: `@Deprecated`, NULL 허용 (점진 제거)

## E2E 테스트 10 시나리오

| # | 시나리오 | 검증 |
|---|---------|------|
| 1 | 예약 생성 → RESERVATION_CONFIRM | 알림톡 Mock 호출 + notifications INSERT |
| 2 | 예약 생성 → 강사 NEW_RESERVATION | recipientType=INSTRUCTOR, recipientId=instructorId |
| 3 | 예약 취소 → RESERVATION_CANCEL | 알림톡 Mock 호출 + notifications INSERT |
| 4 | 알림톡 실패 → SMS 폴백 | FAIL_ prefix → status=FALLBACK_SENT |
| 5 | 알림톡+SMS 모두 실패 | status=FAILED + failureReason 기록 |
| 6 | 리마인드 스케줄러 | REMINDER_1HOUR 발송 |
| 7 | 만료 알림 스케줄러 | MEMBERSHIP_EXPIRING 발송 |
| 8 | 휴강 시 알림 X | Mock 호출 카운트 0 (의뢰인 정책) |
| 9 | 권한 분리 | admin API 403 + 다른 회원 403 + 강사 알림 403 |
| 10 | 관리자 재발송 | FAILED → resend → SENT |

## 빌드 결과
- 전체: 86 tests, 0 failures, 1 skipped (STEP 8 시나리오8 비관적 락)
- 커밋: 13건 (STEP 10 본작업 8건 + 보강 5건)

## STEP 5 회귀 회고
- STEP 4에서 회원 phone 암호화 → STEP 5 강사 도메인에서 미적용 → STEP 10 보강에서 발견
- 교훈: 보안 정책은 도메인 단위가 아닌 **필드 유형** 단위로 통일 검증
