package com.pilates.domain.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출석 응답")
public record AttendanceResponse(
        @Schema(description = "출석 ID") Long id,
        @Schema(description = "예약 ID") Long reservationId,
        @Schema(description = "회원 ID") Long memberId,
        @Schema(description = "회원 이름") String memberName,
        @Schema(description = "수업 ID") Long classScheduleId,
        @Schema(description = "수업 날짜") String classDate,
        @Schema(description = "시작 시간") String startTime,
        @Schema(description = "종료 시간") String endTime,
        @Schema(description = "수업 유형") String lessonTypeName,
        @Schema(description = "강사 이름") String instructorName,
        @Schema(description = "출석 상태") String status,
        @Schema(description = "체크 시각") String checkedAt,
        @Schema(description = "생성 시각") String createdAt
) {
}
