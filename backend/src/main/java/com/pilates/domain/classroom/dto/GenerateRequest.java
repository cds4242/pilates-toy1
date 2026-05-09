package com.pilates.domain.classroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 수업 자동 생성 요청 DTO.
 */
@Schema(description = "수업 자동 생성 요청")
public record GenerateRequest(

        @Schema(description = "생성할 주 수 (1~8, 기본 4)", example = "4")
        @Min(value = 1, message = "최소 1주입니다.")
        @Max(value = 8, message = "최대 8주입니다.")
        Integer weeks
) {
    /** 기본값 4주 적용 */
    public int getWeeksOrDefault() {
        return weeks != null ? weeks : 4;
    }
}
