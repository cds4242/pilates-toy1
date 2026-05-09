package com.pilates.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 결제 승인 요청 DTO.
 */
@Schema(description = "결제 승인 요청 (토스 콜백)")
public record ConfirmRequest(

        @Schema(description = "토스 paymentKey", example = "toss_pk_abc123")
        @NotBlank(message = "paymentKey는 필수입니다.")
        String paymentKey,

        @Schema(description = "주문 번호", example = "ORDER_550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "orderId는 필수입니다.")
        String orderId,

        @Schema(description = "결제 금액", example = "480000")
        @NotNull(message = "결제 금액은 필수입니다.")
        BigDecimal amount
) {
}
