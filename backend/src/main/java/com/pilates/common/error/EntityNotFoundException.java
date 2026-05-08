package com.pilates.common.error;

/**
 * 엔티티 조회 실패 시 발생하는 예외 (404).
 * 도메인별로 직접 사용하거나 상속하여 구체화할 수 있다.
 */
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EntityNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
