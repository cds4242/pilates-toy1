package com.pilates.domain.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "일괄 출석 체크 요청")
public record BatchAttendanceRequest(
        @NotEmpty
        @Valid
        @Schema(description = "출석 항목 리스트")
        List<AttendanceItem> attendances
) {
    @Schema(description = "개별 출석 항목")
    public record AttendanceItem(
            @NotNull
            @Schema(description = "예약 ID", example = "1")
            Long reservationId,

            @NotNull
            @Schema(description = "출석 상태 (ATTENDED, LATE, ABSENT)", example = "ATTENDED")
            String status
    ) {
    }
}
