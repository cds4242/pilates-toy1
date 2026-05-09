package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 정기권 종류-수업 유형 매핑 변경 요청 DTO.
 */
@Schema(description = "수업 유형 매핑 변경 요청")
public record LessonTypeMappingRequest(

        @Schema(description = "수업 유형 ID 목록", example = "[1, 2]")
        @NotEmpty(message = "수업 유형은 1개 이상 필수입니다.")
        List<Long> lessonTypeIds
) {
}
