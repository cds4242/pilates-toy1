package com.pilates.domain.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 수업 시간표 응답 DTO.
 */
@Schema(description = "수업 시간표 응답")
public record ClassScheduleResponse(

        @Schema(description = "수업 ID")
        Long id,

        @Schema(description = "수업 날짜")
        LocalDate classDate,

        @Schema(description = "시작 시간")
        LocalTime startTime,

        @Schema(description = "종료 시간")
        LocalTime endTime,

        @Schema(description = "강사 ID")
        Long instructorId,

        @Schema(description = "강사 이름")
        String instructorName,

        @Schema(description = "수업 유형 ID")
        Long lessonTypeId,

        @Schema(description = "수업 유형 이름")
        String lessonTypeName,

        @Schema(description = "정원")
        Integer maxCapacity,

        @Schema(description = "현재 예약 인원")
        Integer currentCount,

        @Schema(description = "상태 (SCHEDULED/CANCELLED/COMPLETED)")
        String status,

        @Schema(description = "예약 가능 여부")
        boolean reservable
) {
}
