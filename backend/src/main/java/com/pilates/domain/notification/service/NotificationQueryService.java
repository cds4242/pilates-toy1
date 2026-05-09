package com.pilates.domain.notification.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.notification.dto.NotificationResponse;
import com.pilates.domain.notification.dto.NotificationStatisticsResponse;
import com.pilates.domain.notification.entity.Notification;
import com.pilates.domain.notification.entity.NotificationStatus;
import com.pilates.domain.notification.entity.NotificationType;
import com.pilates.domain.notification.entity.RecipientType;
import com.pilates.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 알림 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    /** 회원 본인 알림 목록 */
    public Page<NotificationResponse> getMyNotifications(Long memberId, int page, int size) {
        return notificationRepository.findAllByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
                        RecipientType.MEMBER, memberId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    /** 회원 알림 상세 (본인만) */
    public NotificationResponse getMyNotificationDetail(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (notification.getRecipientType() != RecipientType.MEMBER
                || !notification.getRecipientId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return toResponse(notification);
    }

    /** 관리자: 필터 조회 */
    public Page<NotificationResponse> getAdminNotifications(Long recipientId, String status, String type,
                                                             LocalDateTime from, LocalDateTime to,
                                                             int page, int size) {
        NotificationStatus statusEnum = status != null ? NotificationStatus.valueOf(status) : null;
        NotificationType typeEnum = type != null ? NotificationType.valueOf(type) : null;

        return notificationRepository.findAllWithFilters(recipientId, statusEnum, typeEnum, from, to,
                        PageRequest.of(page, size))
                .map(this::toResponse);
    }

    /** 관리자: 통계 */
    public NotificationStatisticsResponse getStatistics(LocalDateTime from, LocalDateTime to) {
        long sentCount;
        long fallbackSentCount;
        long failedCount;
        long pendingCount;

        if (from != null && to != null) {
            sentCount = notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.SENT, from, to);
            fallbackSentCount = notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.FALLBACK_SENT, from, to);
            failedCount = notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.FAILED, from, to);
            pendingCount = notificationRepository.countByStatusAndCreatedAtBetween(NotificationStatus.PENDING, from, to);
        } else {
            sentCount = notificationRepository.countByStatus(NotificationStatus.SENT);
            fallbackSentCount = notificationRepository.countByStatus(NotificationStatus.FALLBACK_SENT);
            failedCount = notificationRepository.countByStatus(NotificationStatus.FAILED);
            pendingCount = notificationRepository.countByStatus(NotificationStatus.PENDING);
        }

        long totalCount = sentCount + fallbackSentCount + failedCount + pendingCount;
        double successRate = totalCount > 0 ? (sentCount + fallbackSentCount) * 100.0 / totalCount : 0;
        double fallbackRate = totalCount > 0 ? fallbackSentCount * 100.0 / totalCount : 0;

        return new NotificationStatisticsResponse(
                totalCount, sentCount, fallbackSentCount, failedCount, pendingCount,
                Math.round(successRate * 100.0) / 100.0,
                Math.round(fallbackRate * 100.0) / 100.0);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getTemplateCode(),
                n.getContent(),
                n.getStatus().name(),
                n.getChannel() != null ? n.getChannel().name() : null,
                n.getFailureReason(),
                n.getScheduledAt() != null ? n.getScheduledAt().toString() : null,
                n.getSentAt() != null ? n.getSentAt().toString() : null,
                n.getCreatedAt() != null ? n.getCreatedAt().toString() : null
        );
    }
}
