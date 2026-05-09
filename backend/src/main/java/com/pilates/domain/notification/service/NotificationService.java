package com.pilates.domain.notification.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.notification.kakao.AlimtalkResponse;
import com.pilates.common.notification.kakao.KakaoAlimtalkClient;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.common.sms.SmsService;
import com.pilates.domain.instructor.entity.Instructor;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.notification.entity.*;
import com.pilates.domain.notification.repository.NotificationRepository;
import com.pilates.domain.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 알림 발송 서비스.
 * 알림톡 → SMS 폴백 흐름을 처리한다.
 * recipientType에 따라 회원/강사 phone을 조회하여 발송한다.
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
    private final InstructorRepository instructorRepository;

    /**
     * 회원 대상 알림 생성.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotificationForMember(Long memberId, NotificationType type,
                                                     String templateCode, String content,
                                                     LocalDateTime scheduledAt) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Notification notification = Notification.createForMember(member, type, templateCode, content, scheduledAt);
        Notification saved = notificationRepository.saveAndFlush(notification);
        log.info("알림 생성(회원): id={}, type={}, recipientId={}", saved.getId(), type, memberId);
        return saved;
    }

    /**
     * 강사 대상 알림 생성.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotificationForInstructor(Long instructorId, NotificationType type,
                                                        String templateCode, String content,
                                                        LocalDateTime scheduledAt) {
        Notification notification = Notification.createForInstructor(instructorId, type, templateCode, content, scheduledAt);
        Notification saved = notificationRepository.saveAndFlush(notification);
        log.info("알림 생성(강사): id={}, type={}, recipientId={}", saved.getId(), type, instructorId);
        return saved;
    }

    /** @deprecated createNotificationForMember 사용 */
    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotification(Member member, NotificationType type,
                                            String templateCode, String content,
                                            LocalDateTime scheduledAt) {
        Notification notification = Notification.createForMember(member, type, templateCode, content, scheduledAt);
        Notification saved = notificationRepository.saveAndFlush(notification);
        log.info("알림 생성: id={}, type={}, templateCode={}", saved.getId(), type, templateCode);
        return saved;
    }

    /** @deprecated createNotificationForMember 사용 */
    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotificationByMemberId(Long memberId, NotificationType type,
                                                      String templateCode, String content,
                                                      LocalDateTime scheduledAt) {
        return createNotificationForMember(memberId, type, templateCode, content, scheduledAt);
    }

    /**
     * 비동기 알림 발송.
     * recipientType에 따라 phone 조회 → 알림톡 → SMS 폴백.
     */
    @Async("notificationExecutor")
    @Transactional
    public void send(Long notificationId) {
        sendInternal(notificationId);
    }

    /**
     * 동기 발송 (테스트용 / 스케줄러용).
     */
    @Transactional
    public void sendSync(Long notificationId) {
        sendInternal(notificationId);
    }

    /**
     * 관리자 수동 재발송.
     */
    @Transactional
    public void resend(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.resetForResend();
        sendInternal(notificationId);
    }

    // ── internal ──

    private void sendInternal(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (notification.getStatus() != NotificationStatus.PENDING) {
            log.warn("알림 발송 건너뜀: id={}, status={}", notificationId, notification.getStatus());
            return;
        }

        String phone = resolvePhone(notification);
        String content = notification.getContent();
        String templateCode = notification.getTemplateCode();

        // 알림톡 시도
        try {
            AlimtalkResponse response = alimtalkClient.sendAlimtalk(
                    phone, templateCode, Map.of("content", content));

            if (response.success()) {
                notification.markAsSent(NotificationChannel.ALIMTALK, response.messageId());
                log.info("알림톡 발송 성공: notificationId={}, messageId={}", notificationId, response.messageId());
                return;
            }
            log.warn("알림톡 발송 실패, SMS 폴백: notificationId={}, reason={}",
                    notificationId, response.failureReason());
            fallbackToSms(notification, phone, content);
        } catch (Exception e) {
            log.warn("알림톡 발송 예외, SMS 폴백: notificationId={}", notificationId, e);
            fallbackToSms(notification, phone, content);
        }
    }

    /** recipientType에 따라 phone 번호 복호화 조회 */
    private String resolvePhone(Notification notification) {
        if (notification.getRecipientType() == RecipientType.INSTRUCTOR) {
            Instructor instructor = instructorRepository.findById(notification.getRecipientId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUCTOR_NOT_FOUND));
            return encryptionService.decrypt(instructor.getPhoneEncrypted());
        }
        // MEMBER
        Member member = notification.getMember();
        if (member == null) {
            member = memberRepository.findById(notification.getRecipientId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        }
        return encryptionService.decrypt(member.getPhoneEncrypted());
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
