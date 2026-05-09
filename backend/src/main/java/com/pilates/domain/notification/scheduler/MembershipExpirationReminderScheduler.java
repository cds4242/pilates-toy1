package com.pilates.domain.notification.scheduler;

import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipStatus;
import com.pilates.domain.membership.repository.MembershipRepository;
import com.pilates.domain.notification.entity.Notification;
import com.pilates.domain.notification.entity.NotificationType;
import com.pilates.domain.notification.repository.NotificationRepository;
import com.pilates.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 정기권 만료 3일 전 알림 스케줄러.
 * 매일 오전 9시에 실행하여 3일 후 만료 예정인 활성 정기권 보유 회원에게 알림을 발송한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipExpirationReminderScheduler {

    private final MembershipRepository membershipRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EncryptionService encryptionService;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendExpirationReminders() {
        LocalDate threeDaysLater = LocalDate.now().plusDays(3);

        // 3일 후 만료 예정인 활성 정기권 조회
        List<Membership> expiringMemberships = membershipRepository
                .findAllByEndDateAndStatusAndDeletedAtIsNull(threeDaysLater, MembershipStatus.ACTIVE);

        int sentCount = 0;
        for (Membership membership : expiringMemberships) {
            // 이미 알림 발송된 건 제외
            String templateCode = "MEMBERSHIP_EXPIRING_" + membership.getId();
            List<Notification> existing = notificationRepository.findRecentByRecipientAndType(
                    com.pilates.domain.notification.entity.RecipientType.MEMBER,
                    membership.getMember().getId(), NotificationType.MEMBERSHIP_EXPIRING,
                    templateCode, LocalDateTime.now().minusDays(7));
            if (!existing.isEmpty()) {
                continue;
            }

            String memberName = encryptionService.decrypt(membership.getMember().getName());
            String content = String.format("[필라테스 OO점] %s님, 정기권이 3일 후 만료됩니다.", memberName);

            Notification notification = notificationService.createNotificationForMember(
                    membership.getMember().getId(), NotificationType.MEMBERSHIP_EXPIRING,
                    templateCode, content, LocalDateTime.now());
            notificationService.send(notification.getId());
            sentCount++;
        }

        if (sentCount > 0) {
            log.info("정기권 만료 알림 발송: {}건", sentCount);
        }
    }
}
