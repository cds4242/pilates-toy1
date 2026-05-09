package com.pilates.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 결제 준비 응답 DTO.
 */
@Schema(description = "결제 준비 응답")
public record PrepareResponse(

        @Schema(description = "주문 번호", example = "ORDER_550e8400-e29b-41d4-a716-446655440000")
        String orderId,

        @Schema(description = "결제 금액", example = "480000")
        BigDecimal amount,

        @Schema(description = "주문명 (정기권 이름)", example = "필라테스 12회권")
        String orderName
) {
}
