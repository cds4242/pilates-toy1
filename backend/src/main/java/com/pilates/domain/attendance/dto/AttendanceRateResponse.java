package com.pilates.domain.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출석률 응답")
public record AttendanceRateResponse(
        @Schema(description = "출석 횟수") long attendedCount,
        @Schema(description = "결석 횟수") long absentCount,
        @Schema(description = "노쇼 횟수") long noShowCount,
        @Schema(description = "지각 횟수") long lateCount,
        @Schema(description = "전체 횟수 (PENDING 제외)") long totalCount,
        @Schema(description = "출석률 (%)", example = "83.3") double attendanceRate,
        @Schema(description = "기간", example = "30d") String period
) {
}
