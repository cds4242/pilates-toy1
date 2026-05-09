# STEP 8: 예약(Reservation) 도메인 구현

## 작업 일시
2026-05-12

## 작업 요약
예약 생성/취소 + 동시성 안전 + 정기권 차감/복구 + STEP 5/6 TODO 연결 + 노쇼 스케줄러.
시스템의 심장 — 동시성 + 다른 도메인 통합.

## 구현 API: 4개 (누적 76개)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /api/reservations | 예약 생성 (정기권 차감 + 정원 검증) |
| DELETE | /api/reservations/{id} | 예약 취소 (정기권 복구) |
| GET | /api/members/me/reservations | 내 예약 목록 |
| GET | /api/members/me/reservations/{id} | 내 예약 상세 |

## 동시성 전략

| 항목 | 방식 | 검증 |
|------|------|------|
| 정기권 잔여 차감 | 비관적 락 (SELECT FOR UPDATE) | MySQL IT 통과 |
| 수업 정원 카운트 | 낙관적 락 (@Version) | H2 E2E 통과 |
| 예약 중복 방지 | application 검증 + DB 인덱스 | E2E 통과 |
| 시간 겹침 방지 | JPQL 겹침 쿼리 | E2E 통과 |

## STEP 5/6 TODO 연결

| 항목 | 연결 |
|------|------|
| 휴강 → 예약 자동 취소 | ClassScheduleService.cancelClass → reservationService.cancelAllByClassSchedule |
| 회원 시간표 예약 상태 | listByDateRangeWithMyStatus (RESERVED/FULL/NOT_RESERVED) |
| 강사 수업 예약자 리스트 | ClassScheduleDetailResponse + ReservedMemberInfo |
| 무제한권 월 카운팅 | MembershipService.countMonthlyUsage → reservationRepository 실제 구현 |

## E2E 13시나리오

| # | 시나리오 | 결과 |
|---|---------|------|
| 1 | 예약→차감→취소→복구 | 통과 |
| 2 | 중복 예약 → RES_002 | 통과 |
| 3 | 정원 초과 → RES_004 | 통과 |
| 4 | 정기권 없음 → RES_003 | 통과 |
| 5 | 동시 예약 정원 1석 (CountDownLatch) | 통과 |
| 6 | 취소 시간 지남 → RES_006 | 통과 |
| 7 | 시간 겹침 → RES_010 | 통과 |
| 8 | 비관적 락 동시 차감 (MySQL IT) | 통과 |
| 9 | 휴강→3명 예약 취소 + 정기권 복구 | 통과 |
| 10 | 회원 시간표 myReservationStatus | 통과 |
| 11 | 강사 수업 상세 currentCount=2 | 통과 |
| 12 | 노쇼 스케줄러 → NO_SHOW | 통과 |
| 13 | 시간 겹침 에러 코드 + 메시지 | 통과 |

## ErrorCode 10개 (RES_001~010)

## Flyway V8: membership_pass에 created_by/updated_by 추가
