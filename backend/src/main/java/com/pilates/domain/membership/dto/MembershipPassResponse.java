package com.pilates.domain.membership.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * 정기권 종류 응답 DTO (관리자용).
 */
@Schema(description = "정기권 종류 응답")
public record MembershipPassResponse(

        @Schema(description = "정기권 종류 ID")
        Long id,

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

        @Schema(description = "무제한권 월 최대 이용 횟수")
        Integer monthlyLimit,

        @Schema(description = "표시 순서")
        int displayOrder,

        @Schema(description = "수업 유형 목록")
        List<LessonTypeInfo> lessonTypes,

        @Schema(description = "생성일시")
        String createdAt
) {

    /**
     * 수업 유형 정보.
     */
    @Schema(description = "수업 유형 정보")
    public record LessonTypeInfo(

            @Schema(description = "수업 유형 ID")
            Long id,

            @Schema(description = "수업 유형 이름")
            String name
    ) {
    }
}
