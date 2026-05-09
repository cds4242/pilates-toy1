package com.pilates.domain.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원별 노쇼 횟수")
public record NoShowCountResponse(
        @Schema(description = "회원 ID") Long memberId,
        @Schema(description = "노쇼 횟수") long noShowCount
) {
}
