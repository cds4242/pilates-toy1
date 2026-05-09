package com.pilates.domain.notification.event;

/**
 * 예약 생성 이벤트.
 * ReservationService에서 예약 생성 후 publish.
 */
public record ReservationCreatedEvent(
        Long reservationId,
        Long memberId,
        Long instructorId,
        Long classScheduleId,
        String memberName,
        String className,
        String instructorName,
        String classDate,
        String classTime
) {}
