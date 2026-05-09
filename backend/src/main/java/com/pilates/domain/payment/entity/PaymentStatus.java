package com.pilates.domain.payment.entity;

/** 결제 상태 */
public enum PaymentStatus {
    PENDING,        // 결제 대기 (prepare 후)
    COMPLETED,      // 결제 완료 (승인 후)
    FAILED,         // 결제 실패
    REFUNDED,       // 전액 환불
    PARTIAL_REFUND  // 부분 환불
}
