package com.pilates.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 예약 취소 요청 DTO.
 */
@Schema(description = "예약 취소 요청")
public record ReservationCancelRequest(

        @Schema(description = "취소 사유 (선택)")
        String reason
) {
}
