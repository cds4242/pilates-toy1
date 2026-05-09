package com.pilates.domain.notification.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.response.PageResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.notification.dto.NotificationResponse;
import com.pilates.domain.notification.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 알림 조회 API.
 */
@Tag(name = "Notification (Member)", description = "회원 알림 조회")
@RestController
@RequestMapping("/api/members/me/notifications")
@RequiredArgsConstructor
public class MyNotificationController {

    private final NotificationQueryService notificationQueryService;

    @Operation(summary = "내 알림 목록 조회")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> listMyNotifications(
            @LoginMemberAnnotation LoginMember loginMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                PageResponse.from(notificationQueryService.getMyNotifications(loginMember.memberId(), page, size)));
    }

    @Operation(summary = "내 알림 상세 조회")
    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> getMyNotification(
            @LoginMemberAnnotation LoginMember loginMember,
            @PathVariable Long id) {
        return ApiResponse.success(
                notificationQueryService.getMyNotificationDetail(loginMember.memberId(), id));
    }
}
