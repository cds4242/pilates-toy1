package com.pilates.domain.auth.dto;

/**
 * 로그인/토큰 갱신 응답 DTO.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
