package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 정기권 발급 요청 DTO.
 */
@Schema(description = "정기권 발급 요청")
public record MembershipIssueRequest(

        @Schema(description = "회원 ID", example = "1")
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @Schema(description = "총 횟수", example = "12")
        @NotNull(message = "총 횟수는 필수입니다.")
        Integer totalCount,

        @Schema(description = "금액", example = "480000")
        @NotNull(message = "금액은 필수입니다.")
        BigDecimal price,

        @Schema(description = "유효 기간 (일)", example = "180")
        @NotNull(message = "유효 기간은 필수입니다.")
        Integer validityDays,

        @Schema(description = "무제한 여부", example = "false")
        boolean unlimited,

        @Schema(description = "수업 유형 ID 목록", example = "[1, 2]")
        @NotEmpty(message = "수업 유형은 1개 이상 필수입니다.")
        List<Long> lessonTypeIds,

        @Schema(description = "정기권 종류 ID (상품 기반 발급 시)", example = "1")
        Long membershipPassId
) {
}
