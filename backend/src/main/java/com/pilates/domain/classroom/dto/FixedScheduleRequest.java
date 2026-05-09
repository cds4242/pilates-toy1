package com.pilates.domain.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * 고정 스케줄 생성/수정 요청 DTO.
 */
@Schema(description = "고정 스케줄 요청")
public record FixedScheduleRequest(

        @Schema(description = "강사 ID")
        @NotNull(message = "강사 ID는 필수입니다.")
        Long instructorId,

        @Schema(description = "수업 유형 ID")
        @NotNull(message = "수업 유형 ID는 필수입니다.")
        Long lessonTypeId,

        @Schema(description = "요일", example = "MONDAY")
        @NotNull(message = "요일은 필수입니다.")
        DayOfWeek dayOfWeek,

        @Schema(description = "시작 시간", example = "10:00")
        @NotNull(message = "시작 시간은 필수입니다.")
        LocalTime startTime,

        @Schema(description = "종료 시간", example = "10:50")
        @NotNull(message = "종료 시간은 필수입니다.")
        LocalTime endTime
) {
}
