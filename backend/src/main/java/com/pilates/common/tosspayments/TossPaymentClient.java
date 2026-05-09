package com.pilates.common.tosspayments;

import java.math.BigDecimal;

/**
 * 토스페이먼츠 결제 클라이언트 인터페이스.
 * 실제 구현체는 운영 프로파일에서, Mock은 로컬/테스트에서 사용한다.
 */
public interface TossPaymentClient {

    /**
     * 결제 승인 요청.
     *
     * @param paymentKey 토스 paymentKey
     * @param orderId    주문 번호
     * @param amount     결제 금액
     * @return 승인 결과
     */
    TossConfirmResponse confirmPayment(String paymentKey, String orderId, BigDecimal amount);

    /**
     * 결제 취소(환불) 요청.
     *
     * @param paymentKey   토스 paymentKey
     * @param cancelReason 취소 사유
     * @param cancelAmount 취소 금액
     * @return 취소 결과
     */
    TossCancelResponse cancelPayment(String paymentKey, String cancelReason, BigDecimal cancelAmount);
}
