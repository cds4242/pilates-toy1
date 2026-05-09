package com.pilates.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 DTO.
 */
public record TokenRefreshRequest(
        @NotBlank(message = "Refresh Token이 필요합니다.")
        String refreshToken
) {
}
