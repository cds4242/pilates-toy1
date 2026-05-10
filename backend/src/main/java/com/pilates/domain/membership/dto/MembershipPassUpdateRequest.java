package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 정기권 종류 수정 요청 DTO (부분 업데이트).
 */
@Schema(description = "정기권 종류 수정 요청")
public record MembershipPassUpdateRequest(

        @Schema(description = "정기권 이름")
        String name,

        @Schema(description = "금액")
        BigDecimal price,

        @Schema(description = "총 횟수")
        Integer totalCount,

        @Schema(description = "유효 기간 (일)")
        Integer validityDays,

        @Schema(description = "표시 순서")
        Integer displayOrder,

        @Schema(description = "회원 노출 여부")
        Boolean visible,

        @Schema(description = "판매 활성 상태")
        Boolean active,

        @Schema(description = "판매 시작일 (yyyy-MM-dd)")
        String saleStartDate,

        @Schema(description = "판매 종료일 (yyyy-MM-dd, null이면 무기한)")
        String saleEndDate,

        @Schema(description = "카테고리 (PERSONAL/GROUP/UNLIMITED)")
        String category,

        @Schema(description = "상품 설명")
        String description
) {
}
