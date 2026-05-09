package com.pilates.common.notification.kakao;

/**
 * 카카오 알림톡 발송 결과.
 */
public record AlimtalkResponse(
        boolean success,
        String messageId,
        String failureReason
) {
    public static AlimtalkResponse success(String messageId) {
        return new AlimtalkResponse(true, messageId, null);
    }

    public static AlimtalkResponse failure(String reason) {
        return new AlimtalkResponse(false, null, reason);
    }
}
