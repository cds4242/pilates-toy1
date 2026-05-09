package com.pilates.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "휴대폰 번호", example = "010-1234-5678")
        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        String phoneNumber,

        @Schema(description = "비밀번호", example = "Test1234!")
        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}
