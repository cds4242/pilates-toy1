package com.pilates.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 결제 조회 응답 DTO.
 */
@Schema(description = "결제 조회 응답")
public record PaymentResponse(

        @Schema(description = "결제 ID", example = "1")
        Long id,

        @Schema(description = "주문 번호", example = "ORDER_550e8400-e29b-41d4-a716-446655440000")
        String orderId,

        @Schema(description = "결제 금액", example = "480000")
        BigDecimal amount,

        @Schema(description = "결제 수단", example = "CARD")
        String method,

        @Schema(description = "결제 상태", example = "COMPLETED")
        String status,

        @Schema(description = "환불 금액", example = "0")
        BigDecimal refundAmount,

        @Schema(description = "정기권 종류명", example = "필라테스 12회권")
        String membershipPassName,

        @Schema(description = "결제 완료 시각", example = "2026-05-09T14:30:00")
        String paidAt,

        @Schema(description = "결제 생성 시각", example = "2026-05-09T14:25:00")
        String createdAt
) {
}
