package com.pilates.common.error;

/**
 * 잘못된 상태 전이 시 발생하는 예외 (400).
 * 예: 이미 취소된 예약을 다시 취소하려는 경우.
 */
public class InvalidStateException extends BusinessException {

    public InvalidStateException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidStateException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
