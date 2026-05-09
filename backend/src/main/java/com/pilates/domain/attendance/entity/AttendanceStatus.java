package com.pilates.domain.attendance.entity;

/** 출석 상태 */
public enum AttendanceStatus {
    PENDING,    // 대기 (예약 생성 시 자동)
    ATTENDED,   // 출석
    LATE,       // 지각
    ABSENT,     // 결석
    NO_SHOW     // 노쇼 (스케줄러 자동 처리)
}
