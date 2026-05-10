package com.pilates.domain.instructor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 강사 정보 수정 요청 DTO.
 * null인 필드는 변경하지 않는다.
 */
@Schema(description = "강사 정보 수정 요청")
public record InstructorUpdateRequest(

        @Schema(description = "강사 이름")
        String name,

        @Schema(description = "전화번호")
        String phone,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "이메일")
        String email,

        @Schema(description = "주소")
        String address,

        @Schema(description = "생년월일 (yyyy-MM-dd)")
        String birthDate,

        @Schema(description = "전문 분야")
        String specialty,

        @Schema(description = "자격증")
        String certification,

        @Schema(description = "근무 요일 (MON,TUE,...)")
        String workingDays,

        @Schema(description = "메모")
        String memo
) {
}
