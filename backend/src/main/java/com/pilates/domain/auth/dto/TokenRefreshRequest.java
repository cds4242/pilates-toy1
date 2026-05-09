package com.pilates.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 갱신 요청")
public record TokenRefreshRequest(
        @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "Refresh Token이 필요합니다.")
        String refreshToken
) {
}
