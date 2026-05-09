package com.pilates.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 수업 예약자 정보 DTO (강사용 상세 조회).
 */
@Schema(description = "수업 예약자 정보")
public record ReservedMemberInfo(

        @Schema(description = "회원 ID")
        Long memberId,

        @Schema(description = "회원 이름")
        String memberName,

        @Schema(description = "프로필 이미지 URL", nullable = true)
        String profileImageUrl,

        @Schema(description = "예약 상태 (CONFIRMED/NO_SHOW)")
        String reservationStatus
) {
}
