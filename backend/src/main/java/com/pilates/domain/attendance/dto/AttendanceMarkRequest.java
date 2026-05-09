package com.pilates.domain.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "단건 출석 체크 요청")
public record AttendanceMarkRequest(
        @NotNull
        @Schema(description = "출석 상태", example = "ATTENDED")
        String status
) {
}
