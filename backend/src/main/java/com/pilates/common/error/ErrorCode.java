package com.pilates.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드 정의.
 * 도메인별 접두어로 구분한다: COMMON_, MEMBER_, TICKET_, RESERVATION_ 등.
 * 도메인 에러 코드는 해당 도메인 개발 시 추가.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ──
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_002", "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_003", "허용되지 않은 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_004", "요청한 리소스를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_005", "접근 권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_006", "인증이 필요합니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
