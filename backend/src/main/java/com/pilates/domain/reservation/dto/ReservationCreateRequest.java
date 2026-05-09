package com.pilates.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 예약 생성 요청 DTO.
 */
@Schema(description = "예약 생성 요청")
public record ReservationCreateRequest(

        @Schema(description = "수업 시간표 ID", example = "1")
        @NotNull
        Long classScheduleId
) {
}
