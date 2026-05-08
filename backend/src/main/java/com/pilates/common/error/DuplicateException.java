package com.pilates.common.error;

/**
 * 중복 데이터 발생 시 예외 (409).
 * 예: 같은 시간대에 동일 회원 중복 예약.
 */
public class DuplicateException extends BusinessException {

    public DuplicateException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DuplicateException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
