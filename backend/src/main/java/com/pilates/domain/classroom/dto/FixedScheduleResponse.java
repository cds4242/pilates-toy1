package com.pilates.domain.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * 고정 스케줄 응답 DTO.
 */
@Schema(description = "고정 스케줄 응답")
public record FixedScheduleResponse(

        @Schema(description = "고정 스케줄 ID")
        Long id,

        @Schema(description = "강사 ID")
        Long instructorId,

        @Schema(description = "강사 이름")
        String instructorName,

        @Schema(description = "수업 유형 ID")
        Long lessonTypeId,

        @Schema(description = "수업 유형 이름")
        String lessonTypeName,

        @Schema(description = "요일")
        DayOfWeek dayOfWeek,

        @Schema(description = "시작 시간")
        LocalTime startTime,

        @Schema(description = "종료 시간")
        LocalTime endTime,

        @Schema(description = "활성 여부")
        boolean active
) {
}
