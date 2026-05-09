package com.pilates.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 로그인 응답")
public record AdminLoginResponse(
        @Schema(description = "관리자 ID") Long adminId,
        @Schema(description = "역할") String role,
        @Schema(description = "강사 ID (강사인 경우)") Long instructorId,
        @Schema(description = "Access Token") String accessToken,
        @Schema(description = "Refresh Token") String refreshToken,
        @Schema(description = "만료 시간(초)") long expiresIn
) {
}
