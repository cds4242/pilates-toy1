package com.pilates.domain.instructor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * 강사 근무 가능 시간 요청 DTO.
 */
@Schema(description = "강사 근무 가능 시간 요청")
public record AvailableTimeRequest(

        @Schema(description = "요일", example = "MONDAY")
        @NotNull(message = "요일은 필수입니다.")
        DayOfWeek dayOfWeek,

        @Schema(description = "시작 시간", example = "09:00")
        @NotNull(message = "시작 시간은 필수입니다.")
        LocalTime startTime,

        @Schema(description = "종료 시간", example = "18:00")
        @NotNull(message = "종료 시간은 필수입니다.")
        LocalTime endTime
) {
}
