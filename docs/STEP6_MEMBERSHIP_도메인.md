# STEP 6: 정기권(Membership) 도메인 구현

## 작업 일시
2026-05-12 ~

## 작업 요약
정기권(Membership) 발급 + 조회 + 홀딩(일시정지) + 만료 처리.
정기권-수업유형 매핑(MembershipLessonType) + 홀딩 이력(MembershipHolding) 포함.

## 현재 상태

### 이미 존재하는 것
- **Entity**: Membership, MembershipStatus, MembershipLessonType, MembershipHolding
- **DB 테이블**: V1 마이그레이션으로 생성 완료
- **Entity 도메인 메서드**: placeholder (deduct, restore, startHolding, endHolding, expire)
- **동시성**: 비관적 락 `@Lock(PESSIMISTIC_WRITE)` 설계 완료 (미구현)

### 아직 없는 것
- Repository, Service, Controller, DTO
- 도메인 메서드 실제 구현
- 만료 자동 처리 스케줄러
- 결제(Payment) 연동: STEP 7에서

## Phase 구성

| Phase | 내용 | 상태 |
|-------|------|------|
| 1 | Membership Entity 도메인 메서드 구현 | 대기 |
| 2 | Repository + Service + Controller | 대기 |
| 3 | 홀딩(일시정지) + 유효기간 연장 | 대기 |
| 4 | 만료 자동 처리 스케줄러 | 대기 |
| 5 | 통합 테스트 + Swagger | 대기 |

---

## Phase 1: Entity 도메인 메서드

### Membership
```java
deduct(int count)         // 잔여 횟수 차감 (비관적 락 하에서 호출)
restore(int count)        // 잔여 횟수 복구 (취소 시)
startHolding()            // 상태 → HOLDING
endHolding(int days)      // 상태 → ACTIVE, endDate += days
expire()                  // 상태 → EXPIRED
isUsable()                // 활성 + 잔여 > 0 + 유효기간 내
isExpired()               // 종료일 < today
```

### 비즈니스 규칙
- 차감: `remaining_count -= lesson_type.deduction_count`
- 0이 되면 자동 EXHAUSTED
- 무제한권: `is_unlimited=true`, remaining_count 무시, 월 한도는 studio_settings
- 홀딩: 홀딩 일수만큼 end_date 자동 연장
- 1개 정기권이 여러 수업 유형에 사용 가능 (membership_lesson_types 매핑)

---

## Phase 2: CRUD API

### API 설계
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | /api/admin/memberships | 정기권 발급 (관리자) | ADMIN |
| GET | /api/admin/memberships | 정기권 목록 (필터: 회원/상태) | ADMIN |
| GET | /api/admin/memberships/{id} | 정기권 상세 | ADMIN |
| PATCH | /api/admin/memberships/{id} | 정기권 수정 (기간/횟수) | ADMIN |
| GET | /api/members/me/memberships | 내 정기권 목록 (회원) | MEMBER |
| GET | /api/members/me/memberships/{id} | 내 정기권 상세 (회원) | MEMBER |

---

## Phase 3: 홀딩

### API 설계
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | /api/admin/memberships/{id}/hold | 홀딩 시작 | ADMIN |
| POST | /api/admin/memberships/{id}/resume | 홀딩 종료 | ADMIN |
| GET | /api/admin/memberships/{id}/holdings | 홀딩 이력 | ADMIN |

---

## Phase 4: 만료 자동 처리

### MembershipExpirationScheduler
- `@Scheduled(cron = "0 0 1 * * *")` 매일 새벽 1시
- end_date < today && status = ACTIVE → status = EXPIRED
- 만료 임박 알림은 STEP 10(알림 도메인) 후 연결 (TODO)

---

## Phase 5: 통합 테스트

### 시나리오
1. 정기권 발급 → 조회 → 잔여 확인
2. 홀딩 → 유효기간 연장 확인 → 재개
3. 만료 자동 처리 (스케줄러 테스트)
4. 무제한권 발급 → 월 한도 확인 (TODO: reservation 연동 후)

---

## STEP 8 연결 (예약 도메인)
- 예약 시 정기권 차감 (`Membership.deduct`)
- 예약 취소 시 정기권 복구 (`Membership.restore`)
- 수업 유형 매칭 검증 (`MembershipLessonType`)

## 결제 연결 (STEP 7)
- 정기권 발급 시 Payment 생성
- 환불 시 Payment.refund + Membership 상태 변경
