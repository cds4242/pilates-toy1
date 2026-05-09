# STEP 6: 정기권(Membership) 도메인 구현

## 작업 일시
2026-05-12

## 작업 요약
정기권 종류(MembershipPass) 상품 카탈로그 + 회원 정기권(Membership) 발급/조회/일시정지 + 만료 스케줄러.
STEP 3 누락 보강(membership_pass 테이블 V6 마이그레이션) 포함.

## Phase 완료 현황

| Phase | 내용 | 상태 |
|-------|------|------|
| 1 | Membership Entity 도메인 메서드 (deduct/restore/hold/expire) | 완료 |
| 2 | 회원 정기권 CRUD + Service + Controller | 완료 |
| 3 | 일시정지(홀딩) + 유효기간 연장 | 완료 |
| 4 | 만료 자동 처리 스케줄러 | 완료 |
| 5 | STEP 3 누락 보강: MembershipPass 도메인 전체 | 완료 |
| 6 | E2E 테스트 + Swagger + 시드 | 완료 |

## 구현 API: 16개 (누적 63개)

| 그룹 | 수 | 경로 |
|------|---|------|
| 정기권 종류 (Admin) | 6 | /api/admin/membership-passes/** |
| 정기권 종류 (Public) | 2 | /api/membership-passes/** |
| 회원 정기권 (Admin) | 6 | /api/admin/memberships/** |
| 회원 정기권 (Member) | 2 | /api/members/me/memberships/** |

## 시드 데이터 (4종)

| 정기권 | 가격 | 횟수 | 유효기간 | 수업 유형 |
|--------|------|------|---------|----------|
| 8회권 | 180,000 | 8 | 60일 | 그룹, 듀엣 |
| 12회권 | 250,000 | 12 | 90일 | 그룹, 듀엣 |
| 무제한권 | 350,000 | - | 30일 | 그룹 |
| 개인 10회권 | 500,000 | 10 | 90일 | 개인 |

## STEP 7/8 연결 대기
- STEP 7: 환불 처리 + 결제 연동
- STEP 8: 예약 시 차감(deduct)/복구(restore) 호출
