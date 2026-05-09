package com.pilates.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 토스 웹훅 페이로드.
 */
@Schema(description = "토스 웹훅 페이로드")
public record TossWebhookPayload(
        @Schema(description = "이벤트 타입") String eventType,
        @Schema(description = "데이터") Data data
) {
    public record Data(
            @Schema(description = "결제 키") String paymentKey,
            @Schema(description = "주문 ID") String orderId,
            @Schema(description = "상태") String status,
            @Schema(description = "금액") BigDecimal totalAmount
    ) {}
}
