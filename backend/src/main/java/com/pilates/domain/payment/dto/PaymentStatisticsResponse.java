package com.pilates.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 매출 통계 응답 DTO.
 */
@Schema(description = "매출 통계")
public record PaymentStatisticsResponse(
        @Schema(description = "날짜") LocalDate date,
        @Schema(description = "결제 건수") long count,
        @Schema(description = "매출 합계") BigDecimal totalAmount,
        @Schema(description = "환불 합계") BigDecimal totalRefund,
        @Schema(description = "순매출 (매출-환불)") BigDecimal netAmount
) {
}
