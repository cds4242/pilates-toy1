package com.pilates.domain.instructor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 강사 공개 응답 DTO (회원/비회원용).
 */
@Schema(description = "강사 공개 응답")
public record PublicInstructorResponse(

        @Schema(description = "공개 ID")
        String publicId,

        @Schema(description = "강사 이름")
        String name,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl
) {
}
