package com.pilates.domain.auth.dto;

/**
 * SMS 인증번호 검증 성공 응답 DTO.
 */
public record SmsVerifyResponse(
        /** 인증 토큰 (회원가입 시 사용, UUID 32자) */
        String verifiedToken
) {
}
