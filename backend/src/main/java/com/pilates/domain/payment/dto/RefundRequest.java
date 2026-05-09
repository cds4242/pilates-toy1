package com.pilates.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 환불 요청 DTO.
 */
@Schema(description = "환불 요청")
public record RefundRequest(

        @Schema(description = "환불 금액", example = "480000")
        @NotNull(message = "환불 금액은 필수입니다.")
        BigDecimal refundAmount,

        @Schema(description = "환불 사유", example = "고객 요청 환불")
        String reason
) {
}
