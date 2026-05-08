package com.pilates.domain.reservation.entity;

/** 예약 상태 */
public enum ReservationStatus {
    CONFIRMED,  // 예약 확정
    WAITING,    // 대기
    CANCELLED,  // 취소
    NO_SHOW     // 노쇼
}
