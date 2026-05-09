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

    // ── SMS 인증 ──
    SMS_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "SMS_001", "잠시 후 다시 시도해주세요. (1분 내 재발송 불가)"),
    SMS_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "SMS_002", "오늘 인증번호 발송 횟수를 초과했습니다. (일 5회)"),
    SMS_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "SMS_003", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    SMS_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "SMS_004", "인증번호가 일치하지 않습니다."),
    SMS_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "SMS_005", "SMS 인증이 필요합니다."),
    SMS_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SMS_006", "SMS 인증 서비스를 일시적으로 사용할 수 없습니다."),

    // ── 회원 ──
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER_001", "이미 가입된 휴대폰 번호입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_002", "회원을 찾을 수 없습니다."),

    // ── 로그인 ──
    AUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_010", "휴대폰 번호 또는 비밀번호가 올바르지 않습니다."),

    // ── 프로필 이미지 ──
    PROFILE_IMAGE_EMPTY(HttpStatus.BAD_REQUEST, "IMG_001", "이미지 파일을 선택해주세요."),
    PROFILE_IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMG_002", "이미지 크기는 5MB 이하여야 합니다."),
    PROFILE_IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "IMG_003", "JPG, PNG, WebP 형식만 지원합니다."),
    PROFILE_IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMG_004", "이미지 업로드에 실패했습니다."),

    // ── 강사 ──
    INSTRUCTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "INST_001", "강사를 찾을 수 없습니다."),
    INSTRUCTOR_ALREADY_INACTIVE(HttpStatus.BAD_REQUEST, "INST_002", "이미 비활성 상태인 강사입니다."),
    INSTRUCTOR_ALREADY_ACTIVE(HttpStatus.BAD_REQUEST, "INST_003", "이미 활성 상태인 강사입니다."),
    INSTRUCTOR_TIME_OVERLAP(HttpStatus.BAD_REQUEST, "INST_004", "근무 가능 시간이 겹칩니다."),

    // ── 수업 유형 ──
    LESSON_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "LESSON_001", "수업 유형을 찾을 수 없습니다."),
    LESSON_TYPE_DUPLICATE_NAME(HttpStatus.CONFLICT, "LESSON_002", "같은 이름의 수업 유형이 있습니다."),

    // ── 고정 스케줄 ──
    FIXED_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "FIXED_001", "고정 스케줄을 찾을 수 없습니다."),
    FIXED_SCHEDULE_TIME_CONFLICT(HttpStatus.CONFLICT, "FIXED_002", "같은 강사의 시간이 겹칩니다."),
    FIXED_SCHEDULE_OUT_OF_AVAILABLE(HttpStatus.BAD_REQUEST, "FIXED_003", "강사 근무 가능 시간 외입니다."),

    // ── 수업 시간표 ──
    CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "CLASS_001", "수업을 찾을 수 없습니다."),
    CLASS_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "CLASS_002", "이미 취소된 수업입니다."),
    CLASS_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "CLASS_003", "이미 완료된 수업입니다."),
    CLASS_PAST_MODIFICATION(HttpStatus.BAD_REQUEST, "CLASS_004", "과거 수업은 수정할 수 없습니다."),
    CLASS_TIME_CONFLICT(HttpStatus.CONFLICT, "CLASS_005", "해당 강사의 같은 시간대에 이미 수업이 존재합니다."),

    // ── 정기권 ──
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "MSHIP_001", "정기권을 찾을 수 없습니다."),
    MEMBERSHIP_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "MSHIP_002", "활성 상태의 정기권이 아닙니다."),
    MEMBERSHIP_ALREADY_EXPIRED(HttpStatus.BAD_REQUEST, "MSHIP_003", "이미 만료된 정기권입니다."),
    MEMBERSHIP_ALREADY_EXHAUSTED(HttpStatus.BAD_REQUEST, "MSHIP_004", "이미 소진된 정기권입니다."),
    MEMBERSHIP_NOT_HOLDING(HttpStatus.BAD_REQUEST, "MSHIP_005", "일시정지 상태가 아닙니다."),
    MEMBERSHIP_HOLDING_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "MSHIP_006", "일시정지 기간이 올바르지 않습니다."),
    MEMBERSHIP_INSUFFICIENT_COUNT(HttpStatus.BAD_REQUEST, "MSHIP_007", "잔여 횟수가 부족합니다."),

    // ── 정기권 종류 ──
    MEMBERSHIP_PASS_NOT_FOUND(HttpStatus.NOT_FOUND, "MPASS_001", "정기권 종류를 찾을 수 없습니다."),
    MEMBERSHIP_PASS_INVALID_CONFIG(HttpStatus.BAD_REQUEST, "MPASS_002", "무제한권 설정이 올바르지 않습니다."),
    MEMBERSHIP_PASS_LESSON_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "MPASS_003", "수업 유형 매핑이 1개 이상 필요합니다."),
    MEMBERSHIP_PASS_DUPLICATE_NAME(HttpStatus.CONFLICT, "MPASS_004", "같은 이름의 정기권 종류가 있습니다."),
    MEMBERSHIP_PASS_UNLIMITED_LESSON_INVALID(HttpStatus.BAD_REQUEST, "MPASS_005", "무제한권은 그룹 수업만 매핑 가능합니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
