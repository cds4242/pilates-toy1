package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 정기권 홀딩 이력 응답 DTO.
 */
@Schema(description = "정기권 홀딩 이력 응답")
public record MembershipHoldingResponse(

        @Schema(description = "홀딩 이력 ID")
        Long id,

        @Schema(description = "일시정지 시작일")
        LocalDate holdStartDate,

        @Schema(description = "일시정지 종료일")
        LocalDate holdEndDate,

        @Schema(description = "사유")
        String reason,

        @Schema(description = "연장 일수")
        Integer extendedDays,

        @Schema(description = "생성일시")
        String createdAt
) {
}
