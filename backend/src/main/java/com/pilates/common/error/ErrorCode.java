package com.pilates.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드 정의.
 * 도메인별 접두어로 구분한다: COMMON_, AUTH_, MEMBER_, TICKET_, RESERVATION_ 등.
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

    // ── 인증 (AUTH) ──
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증 토큰이 만료되었습니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 인증 토큰입니다."),
    AUTH_TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "AUTH_003", "잘못된 형식의 인증 토큰입니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_004", "해당 작업에 대한 권한이 없습니다."),
    AUTH_PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "AUTH_005", "비밀번호는 최소 8자, 대소문자·숫자·특수문자 중 3종 이상 포함해야 합니다."),

    // ── 암호화 ──
    ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ENC_001", "암호화 처리에 실패했습니다."),
    ENCRYPTION_KEY_VERSION_MISMATCH(HttpStatus.INTERNAL_SERVER_ERROR, "ENC_002", "암호화 키 버전이 일치하지 않습니다."),

    // ── 전화번호 ──
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "PHONE_001", "올바르지 않은 휴대폰 번호입니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
