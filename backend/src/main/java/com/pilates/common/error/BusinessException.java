package com.pilates.common.error;

import lombok.Getter;

/**
 * 비즈니스 예외 추상 클래스.
 * 모든 도메인 예외는 이 클래스를 상속한다.
 * ErrorCode를 통해 HTTP 상태코드, 에러 코드, 메시지를 일관되게 관리.
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
