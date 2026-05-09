package com.pilates.common.tosspayments;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 토스페이먼츠 결제 취소 응답.
 */
public record TossCancelResponse(
        String paymentKey,
        BigDecimal cancelAmount,
        String cancelReason,
        LocalDateTime canceledAt
) {
}
