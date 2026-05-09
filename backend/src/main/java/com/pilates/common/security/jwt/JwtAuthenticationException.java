package com.pilates.common.security.jwt;

import com.pilates.common.error.ErrorCode;
import lombok.Getter;

/**
 * JWT 인증 관련 예외 계층.
 * 토큰 만료, 잘못된 형식, 유효하지 않은 서명 등을 구분한다.
 */
@Getter
public class JwtAuthenticationException extends RuntimeException {

    private final ErrorCode errorCode;

    public JwtAuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public JwtAuthenticationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
