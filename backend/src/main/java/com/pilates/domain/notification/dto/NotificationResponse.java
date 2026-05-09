package com.pilates.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 응답")
public record NotificationResponse(
        @Schema(description = "알림 ID") Long id,
        @Schema(description = "알림 유형") String type,
        @Schema(description = "템플릿 코드") String templateCode,
        @Schema(description = "알림 내용") String content,
        @Schema(description = "발송 상태") String status,
        @Schema(description = "발송 채널") String channel,
        @Schema(description = "실패 사유") String failureReason,
        @Schema(description = "예약 발송 시각") String scheduledAt,
        @Schema(description = "실제 발송 시각") String sentAt,
        @Schema(description = "생성 시각") String createdAt
) {}
