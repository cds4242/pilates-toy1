package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * 정기권 종류 공개 응답 DTO.
 */
@Schema(description = "정기권 종류 공개 응답")
public record PublicMembershipPassResponse(

        @Schema(description = "공개 ID")
        String publicId,

        @Schema(description = "정기권 이름")
        String name,

        @Schema(description = "금액")
        BigDecimal price,

        @Schema(description = "총 횟수 (무제한이면 null)")
        Integer totalCount,

        @Schema(description = "유효 기간 (일)")
        int validityDays,

        @Schema(description = "무제한 여부")
        boolean unlimited,

        @Schema(description = "수업 유형 이름 목록")
        List<String> lessonTypeNames
) {
}
