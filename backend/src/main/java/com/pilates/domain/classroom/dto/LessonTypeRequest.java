package com.pilates.domain.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 수업 유형 생성/수정 요청 DTO.
 */
@Schema(description = "수업 유형 요청")
public record LessonTypeRequest(

        @Schema(description = "수업 유형 이름", example = "개인")
        @NotBlank(message = "수업 유형 이름은 필수입니다.")
        String name,

        @Schema(description = "정원", example = "1")
        @NotNull(message = "정원은 필수입니다.")
        @Min(value = 1, message = "정원은 1 이상이어야 합니다.")
        Integer maxCapacity,

        @Schema(description = "수업 시간(분)", example = "50")
        @NotNull(message = "수업 시간은 필수입니다.")
        @Min(value = 1, message = "수업 시간은 1분 이상이어야 합니다.")
        Integer durationMinutes,

        @Schema(description = "정기권 차감 횟수", example = "1")
        @NotNull(message = "차감 횟수는 필수입니다.")
        @Min(value = 0, message = "차감 횟수는 0 이상이어야 합니다.")
        Integer deductionCount
) {
}
