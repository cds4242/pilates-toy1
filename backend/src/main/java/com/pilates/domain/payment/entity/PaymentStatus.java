package com.pilates.domain.payment.entity;

/** 결제 상태 */
public enum PaymentStatus {
    COMPLETED,      // 결제 완료
    REFUNDED,       // 전액 환불
    PARTIAL_REFUND  // 부분 환불
}
