package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 정기권 일시정지 요청 DTO.
 */
@Schema(description = "정기권 일시정지 요청")
public record MembershipHoldRequest(

        @Schema(description = "일시정지 시작일", example = "2026-06-01")
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate fromDate,

        @Schema(description = "일시정지 종료일", example = "2026-06-15")
        @NotNull(message = "종료일은 필수입니다.")
        LocalDate toDate,

        @Schema(description = "사유", example = "해외 출장")
        String reason
) {
}
