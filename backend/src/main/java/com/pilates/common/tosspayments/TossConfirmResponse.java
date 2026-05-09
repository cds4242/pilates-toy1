package com.pilates.common.tosspayments;

import java.math.BigDecimal;

/**
 * 토스페이먼츠 결제 승인 응답.
 */
public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        BigDecimal totalAmount,
        String method,
        String cardCompany
) {
}
