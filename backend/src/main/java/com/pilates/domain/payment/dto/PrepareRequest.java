package com.pilates.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 결제 준비 요청 DTO.
 */
@Schema(description = "결제 준비 요청")
public record PrepareRequest(

        @Schema(description = "정기권 종류 ID", example = "1")
        @NotNull(message = "정기권 종류 ID는 필수입니다.")
        Long membershipPassId
) {
}
