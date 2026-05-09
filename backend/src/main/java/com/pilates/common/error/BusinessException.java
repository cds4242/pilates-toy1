package com.pilates.common.error;

import lombok.Getter;

/**
 * 비즈니스 예외 기본 클래스.
 * ErrorCode를 통해 HTTP 상태코드, 에러 코드, 메시지를 일관되게 관리.
 * 직접 사용하거나, 도메인별 하위 클래스(DuplicateException, InvalidStateException 등)를 사용한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
