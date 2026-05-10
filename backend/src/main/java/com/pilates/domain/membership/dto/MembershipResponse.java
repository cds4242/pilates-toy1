package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 정기권 응답 DTO.
 */
@Schema(description = "정기권 응답")
public record MembershipResponse(

        @Schema(description = "정기권 ID")
        Long id,

        @Schema(description = "공개 ID")
        String publicId,

        @Schema(description = "회원 ID")
        Long memberId,

        @Schema(description = "회원 이름 (관리자 조회 시)")
        String memberName,

        @Schema(description = "정기권 종류 이름")
        String passName,

        @Schema(description = "총 횟수")
        Integer totalCount,

        @Schema(description = "잔여 횟수")
        Integer remainingCount,

        @Schema(description = "무제한 여부")
        boolean unlimited,

        @Schema(description = "시작일")
        LocalDate startDate,

        @Schema(description = "종료일")
        LocalDate endDate,

        @Schema(description = "금액")
        BigDecimal price,

        @Schema(description = "상태 (ACTIVE/EXPIRED/EXHAUSTED/HOLDING)")
        String status,

        @Schema(description = "수업 유형 이름 목록")
        List<String> lessonTypeNames,

        @Schema(description = "사용 가능 여부")
        boolean usable
) {
}
