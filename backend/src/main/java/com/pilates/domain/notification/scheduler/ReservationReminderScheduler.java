package com.pilates.domain.notification.scheduler;

import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.entity.ClassScheduleStatus;
import com.pilates.domain.classroom.repository.ClassScheduleRepository;
import com.pilates.domain.notification.entity.Notification;
import com.pilates.domain.notification.entity.NotificationType;
import com.pilates.domain.notification.repository.NotificationRepository;
import com.pilates.domain.notification.service.NotificationService;
import com.pilates.domain.reservation.entity.Reservation;
import com.pilates.domain.reservation.entity.ReservationStatus;
import com.pilates.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 수업 1시간 전 리마인드 알림 스케줄러.
 * 10분마다 실행하여 1시간 후 시작하는 CONFIRMED 예약에 대해 리마인드 알림을 발송한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationReminderScheduler {

    private final ClassScheduleRepository classScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EncryptionService encryptionService;

    @Scheduled(fixedRate = 600_000)
    @Transactional
    public void sendReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime oneHourLater = now.plusHours(1);

        // 자정 근처 방어
        if (oneHourLater.isBefore(now)) {
            return;
        }

        // 오늘 수업 중 시작 시간이 now ~ now+1h인 SCHEDULED 상태 수업 조회
        List<ClassSchedule> upcomingClasses = classScheduleRepository
                .findAllByClassDateBetweenAndStatusNot(today, today, ClassScheduleStatus.CANCELLED)
                .stream()
                .filter(cs -> cs.getStatus() == ClassScheduleStatus.SCHEDULED)
                .filter(cs -> !cs.getStartTime().isBefore(now) && !cs.getStartTime().isAfter(oneHourLater))
                .toList();

        int sentCount = 0;
        for (ClassSchedule cs : upcomingClasses) {
            List<Reservation> confirmedReservations = reservationRepository
                    .findAllByClassScheduleIdAndStatusIn(cs.getId(), List.of(ReservationStatus.CONFIRMED));

            for (Reservation reservation : confirmedReservations) {
                // 이미 리마인드 발송된 건 제외
                String templateCode = "REMINDER_1HOUR_" + cs.getId() + "_" + reservation.getMember().getId();
                List<Notification> existing = notificationRepository.findRecentByRecipientAndType(
                        com.pilates.domain.notification.entity.RecipientType.MEMBER,
                        reservation.getMember().getId(), NotificationType.REMINDER_1HOUR,
                        templateCode, LocalDateTime.now().minusHours(2));
                if (!existing.isEmpty()) {
                    continue;
                }

                String memberName = encryptionService.decrypt(reservation.getMember().getName());
                String content = String.format("[필라테스 OO점] %s님, %s %s 수업이 1시간 후 시작됩니다.",
                        memberName, cs.getStartTime(), cs.getLessonType().getName());

                Notification notification = notificationService.createNotificationForMember(
                        reservation.getMember().getId(), NotificationType.REMINDER_1HOUR,
                        templateCode, content, LocalDateTime.now());
                notificationService.send(notification.getId());
                sentCount++;
            }
        }

        if (sentCount > 0) {
            log.info("리마인드 알림 발송: {}건", sentCount);
        }
    }
}
