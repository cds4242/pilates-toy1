package com.pilates.domain.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 수업 유형 응답 DTO.
 */
@Schema(description = "수업 유형 응답")
public record LessonTypeResponse(

        @Schema(description = "수업 유형 ID")
        Long id,

        @Schema(description = "수업 유형 이름")
        String name,

        @Schema(description = "정원")
        Integer maxCapacity,

        @Schema(description = "수업 시간(분)")
        Integer durationMinutes,

        @Schema(description = "정기권 차감 횟수")
        Integer deductionCount,

        @Schema(description = "활성 여부")
        boolean active
) {
}
