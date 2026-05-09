package com.pilates.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO.
 */
public record LoginRequest(
        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        String phoneNumber,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}
