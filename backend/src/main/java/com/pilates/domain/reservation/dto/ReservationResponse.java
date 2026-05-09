package com.pilates.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 예약 응답 DTO.
 */
@Schema(description = "예약 응답")
public record ReservationResponse(

        @Schema(description = "예약 ID")
        Long id,

        @Schema(description = "수업 시간표 ID")
        Long classScheduleId,

        @Schema(description = "수업 날짜")
        String classDate,

        @Schema(description = "시작 시간")
        String startTime,

        @Schema(description = "종료 시간")
        String endTime,

        @Schema(description = "수업 유형 이름")
        String lessonTypeName,

        @Schema(description = "강사 이름")
        String instructorName,

        @Schema(description = "예약 상태")
        String status,

        @Schema(description = "정기권 잔여 횟수 (무제한이면 null)")
        Integer remainingCount,

        @Schema(description = "예약 일시")
        String createdAt
) {
}
