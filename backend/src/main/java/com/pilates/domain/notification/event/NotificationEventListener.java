package com.pilates.domain.notification.event;

import com.pilates.domain.notification.entity.Notification;
import com.pilates.domain.notification.entity.NotificationType;
import com.pilates.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * 알림 이벤트 리스너.
 * 트랜잭션 커밋 후 알림 생성 + 비동기 발송.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationCreated(ReservationCreatedEvent event) {
        log.info("예약 생성 알림 이벤트: reservationId={}, memberId={}, instructorId={}",
                event.reservationId(), event.memberId(), event.instructorId());

        try {
            // 1. 회원에게 예약 확인 알림
            String confirmContent = String.format("[필라테스 OO점] %s님, %s %s %s 예약이 완료되었습니다.",
                    event.memberName(), event.classDate(), event.classTime(), event.className());
            Notification confirmNotif = notificationService.createNotificationForMember(
                    event.memberId(), NotificationType.RESERVATION_CONFIRM,
                    "RESERVATION_CONFIRM", confirmContent, LocalDateTime.now());
            notificationService.send(confirmNotif.getId());

            // 2. 강사에게 새 예약 알림
            String instructorContent = String.format("[필라테스 OO점] %s 강사님, %s님이 %s %s 수업을 예약했습니다.",
                    event.instructorName(), event.memberName(), event.classDate(), event.classTime());
            Notification instrNotif = notificationService.createNotificationForInstructor(
                    event.instructorId(), NotificationType.NEW_RESERVATION,
                    "NEW_RESERVATION", instructorContent, LocalDateTime.now());
            notificationService.send(instrNotif.getId());

        } catch (Exception e) {
            log.error("예약 생성 알림 처리 실패: reservationId={}", event.reservationId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationCancelled(ReservationCancelledEvent event) {
        log.info("예약 취소 알림 이벤트: reservationId={}, memberId={}", event.reservationId(), event.memberId());

        try {
            String content = String.format("[필라테스 OO점] %s님, %s %s 예약이 취소되었습니다.",
                    event.memberName(), event.classDate(), event.classTime());
            Notification notification = notificationService.createNotificationForMember(
                    event.memberId(), NotificationType.RESERVATION_CANCEL,
                    "RESERVATION_CANCEL", content, LocalDateTime.now());
            notificationService.send(notification.getId());

        } catch (Exception e) {
            log.error("예약 취소 알림 처리 실패: reservationId={}", event.reservationId(), e);
        }
    }
}
