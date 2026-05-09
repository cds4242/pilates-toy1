package com.pilates.domain.notification.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.notification.kakao.AlimtalkResponse;
import com.pilates.common.notification.kakao.KakaoAlimtalkClient;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.common.sms.SmsService;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.notification.entity.*;
import com.pilates.domain.notification.repository.NotificationRepository;
import com.pilates.domain.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 알림 발송 서비스.
 * 알림톡 → SMS 폴백 흐름을 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final KakaoAlimtalkClient alimtalkClient;
    private final SmsService smsService;
    private final EncryptionService encryptionService;
    private final MemberRepository memberRepository;

    /**
     * 알림 생성 + 저장.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Notification createNotification(Member member, NotificationType type,
                                            String templateCode, String content,
                                            LocalDateTime scheduledAt) {
        Notification notification = Notification.create(member, type, templateCode, content, scheduledAt);
        Notification saved = notificationRepository.saveAndFlush(notification);
        log.info("알림 생성: id={}, type={}, templateCode={}", saved.getId(), type, templateCode);
        return saved;
    }

    /**
     * memberId로 알림 생성 (이벤트 리스너용).
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Notification createNotificationByMemberId(Long memberId, NotificationType type,
                                                      String templateCode, String content,
                                                      LocalDateTime scheduledAt) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Notification notification = Notification.create(member, type, templateCode, content, scheduledAt);
        Notification saved = notificationRepository.saveAndFlush(notification);
        log.info("알림 생성: id={}, type={}, templateCode={}", saved.getId(), type, templateCode);
        return saved;
    }

    /**
     * 비동기 알림 발송.
     * 알림톡 시도 → 실패 시 SMS 폴백.
     */
    @Async("notificationExecutor")
    @Transactional
    public void send(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (notification.getStatus() != NotificationStatus.PENDING) {
            log.warn("알림 발송 건너뜀: id={}, status={}", notificationId, notification.getStatus());
            return;
        }

        Member member = notification.getMember();
        String phone = encryptionService.decrypt(member.getPhoneEncrypted());
        String content = notification.getContent();
        String templateCode = notification.getTemplateCode();

        // 1. 알림톡 시도
        try {
            AlimtalkResponse response = alimtalkClient.sendAlimtalk(
                    phone, templateCode, Map.of("content", content));

            if (response.success()) {
                notification.markAsSent(NotificationChannel.ALIMTALK, response.messageId());
                log.info("알림톡 발송 성공: notificationId={}, messageId={}", notificationId, response.messageId());
                return;
            }
            // 알림톡 실패 → SMS 폴백
            log.warn("알림톡 발송 실패, SMS 폴백 시도: notificationId={}, reason={}",
                    notificationId, response.failureReason());
            fallbackToSms(notification, phone, content);
        } catch (Exception e) {
            log.warn("알림톡 발송 예외, SMS 폴백 시도: notificationId={}", notificationId, e);
            fallbackToSms(notification, phone, content);
        }
    }

    /**
     * 동기 발송 (테스트용 / 스케줄러용).
     */
    @Transactional
    public void sendSync(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (notification.getStatus() != NotificationStatus.PENDING) {
            return;
        }

        Member member = notification.getMember();
        String phone = encryptionService.decrypt(member.getPhoneEncrypted());
        String content = notification.getContent();
        String templateCode = notification.getTemplateCode();

        try {
            AlimtalkResponse response = alimtalkClient.sendAlimtalk(
                    phone, templateCode, Map.of("content", content));

            if (response.success()) {
                notification.markAsSent(NotificationChannel.ALIMTALK, response.messageId());
                return;
            }
            fallbackToSms(notification, phone, content);
        } catch (Exception e) {
            fallbackToSms(notification, phone, content);
        }
    }

    /**
     * 관리자 수동 재발송.
     */
    @Transactional
    public void resend(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.resetForResend();
        sendSync(notificationId);
    }

    private void fallbackToSms(Notification notification, String phone, String content) {
        try {
            smsService.send(phone, content);
            String smsMessageId = "sms-" + UUID.randomUUID().toString().substring(0, 8);
            notification.markAsFallbackSent(smsMessageId);
            log.info("SMS 폴백 발송 성공: notificationId={}", notification.getId());
        } catch (Exception e) {
            notification.markAsFailed("알림톡 실패 + SMS 폴백 실패: " + e.getMessage());
            log.error("SMS 폴백 발송 실패: notificationId={}", notification.getId(), e);
        }
    }
}
