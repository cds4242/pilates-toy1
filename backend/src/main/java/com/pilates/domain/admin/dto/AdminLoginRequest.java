package com.pilates.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "관리자 로그인 요청")
public record AdminLoginRequest(
        @NotBlank @Schema(description = "로그인 ID", example = "admin") String loginId,
        @NotBlank @Schema(description = "비밀번호", example = "admin1234") String password
) {
}
