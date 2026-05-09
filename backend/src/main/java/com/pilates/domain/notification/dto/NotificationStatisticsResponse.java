package com.pilates.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 통계 응답")
public record NotificationStatisticsResponse(
        @Schema(description = "전체 발송 건수") long totalCount,
        @Schema(description = "알림톡 성공 건수") long sentCount,
        @Schema(description = "SMS 폴백 성공 건수") long fallbackSentCount,
        @Schema(description = "실패 건수") long failedCount,
        @Schema(description = "대기 건수") long pendingCount,
        @Schema(description = "성공률 (%)") double successRate,
        @Schema(description = "폴백률 (%)") double fallbackRate
) {}
