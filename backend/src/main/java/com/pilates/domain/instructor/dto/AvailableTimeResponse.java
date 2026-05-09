package com.pilates.domain.instructor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * 강사 근무 가능 시간 응답 DTO.
 */
@Schema(description = "강사 근무 가능 시간 응답")
public record AvailableTimeResponse(

        @Schema(description = "ID")
        Long id,

        @Schema(description = "요일")
        DayOfWeek dayOfWeek,

        @Schema(description = "시작 시간")
        LocalTime startTime,

        @Schema(description = "종료 시간")
        LocalTime endTime
) {
}
