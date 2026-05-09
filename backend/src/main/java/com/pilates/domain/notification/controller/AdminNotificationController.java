package com.pilates.domain.notification.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.response.PageResponse;
import com.pilates.domain.notification.dto.NotificationResponse;
import com.pilates.domain.notification.dto.NotificationStatisticsResponse;
import com.pilates.domain.notification.service.NotificationQueryService;
import com.pilates.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 관리자 알림 관리 API.
 */
@Tag(name = "Notification (Admin)", description = "관리자 알림 관리")
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회 (필터)")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> listNotifications(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                PageResponse.from(notificationQueryService.getAdminNotifications(
                        memberId, status, type, from, to, page, size)));
    }

    @Operation(summary = "알림 통계")
    @GetMapping("/statistics")
    public ApiResponse<NotificationStatisticsResponse> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.success(notificationQueryService.getStatistics(from, to));
    }

    @Operation(summary = "실패한 알림 재발송")
    @PostMapping("/{id}/resend")
    public ApiResponse<Void> resendNotification(@PathVariable Long id) {
        notificationService.resend(id);
        return ApiResponse.success();
    }
}
