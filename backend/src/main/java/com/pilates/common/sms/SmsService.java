package com.pilates.common.sms;

/**
 * SMS 발송 서비스 인터페이스.
 * 운영: 실제 SMS API 연동, 개발: MockSmsService.
 */
public interface SmsService {

    /**
     * SMS 발송.
     * @param phoneNumber 수신 번호 (정규화된 11자리)
     * @param message 메시지 내용
     */
    void send(String phoneNumber, String message);
}
