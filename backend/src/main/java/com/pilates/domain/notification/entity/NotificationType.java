package com.pilates.domain.notification.entity;

/** 알림 유형 */
public enum NotificationType {
    RESERVATION_CONFIRM,    // 예약 확정
    RESERVATION_CANCEL,     // 예약 취소
    REMINDER_1HOUR,         // 수업 1시간 전 리마인더
    NEW_RESERVATION,        // 강사에게 새 예약 알림
    MEMBERSHIP_EXPIRING     // 정기권 만료 임박
}
