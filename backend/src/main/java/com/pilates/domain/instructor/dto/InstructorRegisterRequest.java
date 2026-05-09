package com.pilates.domain.instructor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 강사 등록 요청 DTO.
 */
@Schema(description = "강사 등록 요청")
public record InstructorRegisterRequest(

        @Schema(description = "강사 이름", example = "박지영")
        @NotBlank(message = "강사 이름은 필수입니다.")
        String name,

        @Schema(description = "전화번호", example = "010-1234-5678")
        String phone,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl
) {
}
