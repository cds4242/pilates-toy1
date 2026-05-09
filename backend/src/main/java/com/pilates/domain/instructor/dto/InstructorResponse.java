package com.pilates.domain.instructor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 강사 상세 응답 DTO (관리자용).
 */
@Schema(description = "강사 상세 응답 (관리자)")
public record InstructorResponse(

        @Schema(description = "강사 ID")
        Long id,

        @Schema(description = "공개 ID")
        String publicId,

        @Schema(description = "강사 이름")
        String name,

        @Schema(description = "전화번호")
        String phone,

        @Schema(description = "상태 (ACTIVE/INACTIVE)")
        String status,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "등록일시")
        String createdAt
) {
}
