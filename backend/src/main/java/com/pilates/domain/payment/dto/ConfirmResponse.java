package com.pilates.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 결제 승인 응답 DTO.
 */
@Schema(description = "결제 승인 응답")
public record ConfirmResponse(

        @Schema(description = "결제 ID", example = "1")
        Long paymentId,

        @Schema(description = "발급된 정기권 ID", example = "10")
        Long membershipId,

        @Schema(description = "결제 상태", example = "COMPLETED")
        String status
) {
}
