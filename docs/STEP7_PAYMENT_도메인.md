# STEP 7: 결제(Payment) 도메인 구현

## 작업 일시
2026-05-12

## 작업 요약
토스페이먼츠 V2 연동 + 멱등성 + 정기권 발급 트랜잭션 + 환불 + 웹훅.

## 구현 API: 9개 (누적 72개)

| 그룹 | 수 | 경로 |
|------|---|------|
| Payment (Member) | 4 | prepare, confirm, me/payments |
| Payment (Admin) | 4 | list, detail, refund, statistics |
| Toss Webhook | 1 | /api/webhooks/toss |

## 보안 핵심
- 금액 위변조: PAY_003, 서버에서 MembershipPass 가격 재계산
- 멱등성: orderId UNIQUE + PAY_002
- 보상 트랜잭션: 정기권 발급 실패 → cancelPayment 자동 호출
- 웹훅: HMAC-SHA256 시그니처 + Redis 멱등성 24시간
- 환불 사용분: 사용횟수/총횟수 비례 차감

## E2E 8시나리오 전체 통과
1. 정상 플로우 (prepare→confirm→정기권)
2. 멱등성 (같은 orderId → PAY_002)
3. 금액 위변조 (PAY_003)
4. 전액 환불 → REFUNDED
5. 토스 승인 실패 (FAIL_ prefix → PAY_004)
6. 부분 환불 + 초과 거부 (PAY_007)
7. 웹훅 시그니처 (없음→401, 잘못→401, 정상→200, 멱등)
8. 보상 트랜잭션 (cancelCallCount 검증)
