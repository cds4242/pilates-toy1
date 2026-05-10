package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 정기권 종류 생성 요청 DTO.
 */
@Schema(description = "정기권 종류 생성 요청")
public record MembershipPassCreateRequest(

        @Schema(description = "정기권 이름", example = "12회권")
        @NotBlank(message = "정기권 이름은 필수입니다.")
        String name,

        @Schema(description = "금액", example = "250000")
        @NotNull(message = "금액은 필수입니다.")
        BigDecimal price,

        @Schema(description = "총 횟수 (무제한권이면 null)", example = "12")
        Integer totalCount,

        @Schema(description = "유효 기간 (일)", example = "90")
        @NotNull(message = "유효 기간은 필수입니다.")
        @Min(value = 1, message = "유효 기간은 1일 이상이어야 합니다.")
        Integer validityDays,

        @Schema(description = "무제한 여부", example = "false")
        boolean unlimited,

        @Schema(description = "무제한권 월 최대 이용 횟수", example = "30")
        Integer monthlyLimit,

        @Schema(description = "표시 순서", example = "1")
        int displayOrder,

        @Schema(description = "회원 노출 여부", example = "true")
        Boolean visible,

        @Schema(description = "판매 시작일 (null이면 즉시)", example = "2026-05-10")
        String saleStartDate,

        @Schema(description = "판매 종료일 (null이면 무기한)")
        String saleEndDate,

        @Schema(description = "카테고리 (PERSONAL/GROUP/UNLIMITED)", example = "GROUP")
        String category,

        @Schema(description = "상품 설명", example = "주 2~3회 추천")
        String description,

        @Schema(description = "수업 유형 ID 목록", example = "[1, 2]")
        @NotEmpty(message = "수업 유형은 1개 이상 필수입니다.")
        List<Long> lessonTypeIds
) {
}
