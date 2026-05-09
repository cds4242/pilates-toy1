package com.pilates.common.tosspayments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 토스페이먼츠 Mock 클라이언트.
 * 로컬/테스트 환경에서 실제 PG 연동 없이 결제 승인/취소를 시뮬레이션한다.
 */
@Slf4j
@Component
@Profile({"local", "local-h2", "test"})
public class MockTossPaymentClient implements TossPaymentClient {

    @Override
    public TossConfirmResponse confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        log.info("[MOCK TOSS] 결제 승인 요청 — paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);

        TossConfirmResponse response = new TossConfirmResponse(
                paymentKey,
                orderId,
                "DONE",
                amount,
                "카드",
                "Mock카드사"
        );

        log.info("[MOCK TOSS] 결제 승인 성공 — orderId={}, status={}", orderId, response.status());
        return response;
    }

    @Override
    public TossCancelResponse cancelPayment(String paymentKey, String cancelReason, BigDecimal cancelAmount) {
        log.info("[MOCK TOSS] 결제 취소 요청 — paymentKey={}, reason={}, amount={}", paymentKey, cancelReason, cancelAmount);

        TossCancelResponse response = new TossCancelResponse(
                paymentKey,
                cancelAmount,
                cancelReason,
                LocalDateTime.now()
        );

        log.info("[MOCK TOSS] 결제 취소 성공 — paymentKey={}, cancelAmount={}", paymentKey, cancelAmount);
        return response;
    }
}
