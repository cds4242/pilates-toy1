package com.pilates.domain.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 수업 단건 생성 요청 DTO.
 */
@Schema(description = "수업 단건 생성 요청")
public record ClassScheduleCreateRequest(

        @Schema(description = "강사 ID")
        @NotNull(message = "강사 ID는 필수입니다.")
        Long instructorId,

        @Schema(description = "수업 유형 ID")
        @NotNull(message = "수업 유형 ID는 필수입니다.")
        Long lessonTypeId,

        @Schema(description = "수업 날짜", example = "2026-06-01")
        @NotNull(message = "수업 날짜는 필수입니다.")
        LocalDate classDate,

        @Schema(description = "시작 시간", example = "10:00")
        @NotNull(message = "시작 시간은 필수입니다.")
        LocalTime startTime,

        @Schema(description = "종료 시간", example = "10:50")
        @NotNull(message = "종료 시간은 필수입니다.")
        LocalTime endTime,

        @Schema(description = "정원", example = "1")
        @NotNull(message = "정원은 필수입니다.")
        @Min(value = 1, message = "정원은 1 이상이어야 합니다.")
        Integer maxCapacity
) {
}
