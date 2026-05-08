package com.pilates.domain.notification.entity;

/** 알림 유형 */
public enum NotificationType {
    RESERVATION_CONFIRM,    // 예약 확정
    REMINDER_1DAY,          // 수업 전날 리마인더
    REMINDER_2HOUR,         // 수업 당일 2시간 전 리마인더
    CANCELLATION,           // 취소/변경 알림
    MEMBERSHIP_EXPIRING,    // 정기권 만료 임박
    MEMBERSHIP_LOW          // 정기권 잔여 횟수 부족
}
