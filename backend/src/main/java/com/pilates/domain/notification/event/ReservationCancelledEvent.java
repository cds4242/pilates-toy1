package com.pilates.domain.notification.event;

/**
 * 예약 취소 이벤트.
 * ReservationService에서 예약 취소 후 publish.
 */
public record ReservationCancelledEvent(
        Long reservationId,
        Long memberId,
        String memberName,
        String classDate,
        String classTime
) {}
