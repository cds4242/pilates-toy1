package com.pilates.common.tosspayments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 토스페이먼츠 Mock 클라이언트.
 * 로컬/테스트 환경에서 실제 PG 연동 없이 결제 승인/취소를 시뮬레이션한다.
 *
 * 실패 모드: paymentKey prefix로 제어
 * - "FAIL_" prefix → confirmPayment 시 RuntimeException
 * - 그 외 → 정상 응답
 *
 * cancelPayment 호출 카운트를 추적하여 보상 트랜잭션 검증에 활용.
 */
@Slf4j
@Component
@Profile({"local", "local-h2", "test"})
public class MockTossPaymentClient implements TossPaymentClient {

    /** cancelPayment 호출 카운트 (테스트 검증용) */
    private final AtomicInteger cancelCallCount = new AtomicInteger(0);

    @Override
    public TossConfirmResponse confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        log.info("[MOCK TOSS] 결제 승인 요청 — paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);

        // 실패 모드: paymentKey가 "FAIL_"로 시작하면 예외
        if (paymentKey != null && paymentKey.startsWith("FAIL_")) {
            log.info("[MOCK TOSS] 결제 승인 실패 (의도적) — paymentKey={}", paymentKey);
            throw new RuntimeException("Mock 토스 승인 실패: " + paymentKey);
        }

        TossConfirmResponse response = new TossConfirmResponse(
                paymentKey, orderId, "DONE", amount, "카드", "Mock카드사");

        log.info("[MOCK TOSS] 결제 승인 성공 — orderId={}", orderId);
        return response;
    }

    @Override
    public TossCancelResponse cancelPayment(String paymentKey, String cancelReason, BigDecimal cancelAmount) {
        log.info("[MOCK TOSS] 결제 취소 요청 — paymentKey={}, reason={}, amount={}", paymentKey, cancelReason, cancelAmount);
        cancelCallCount.incrementAndGet();

        TossCancelResponse response = new TossCancelResponse(
                paymentKey, cancelAmount, cancelReason, LocalDateTime.now());

        log.info("[MOCK TOSS] 결제 취소 성공 — paymentKey={}", paymentKey);
        return response;
    }

    /** cancelPayment 호출 카운트 조회 (테스트 검증용) */
    public int getCancelCallCount() {
        return cancelCallCount.get();
    }

    /** cancelPayment 호출 카운트 초기화 (테스트 간 격리용) */
    public void resetCancelCallCount() {
        cancelCallCount.set(0);
    }
}
