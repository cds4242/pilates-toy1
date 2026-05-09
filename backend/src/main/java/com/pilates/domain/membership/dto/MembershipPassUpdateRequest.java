package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 정기권 종류 수정 요청 DTO (부분 업데이트).
 */
@Schema(description = "정기권 종류 수정 요청")
public record MembershipPassUpdateRequest(

        @Schema(description = "정기권 이름", example = "12회권")
        String name,

        @Schema(description = "금액", example = "250000")
        BigDecimal price,

        @Schema(description = "총 횟수", example = "12")
        Integer totalCount,

        @Schema(description = "유효 기간 (일)", example = "90")
        Integer validityDays,

        @Schema(description = "표시 순서", example = "2")
        Integer displayOrder
) {
}
