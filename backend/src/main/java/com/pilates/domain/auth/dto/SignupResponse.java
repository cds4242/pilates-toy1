package com.pilates.domain.auth.dto;

/**
 * 회원가입 성공 응답 DTO.
 */
public record SignupResponse(
        String publicId,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
