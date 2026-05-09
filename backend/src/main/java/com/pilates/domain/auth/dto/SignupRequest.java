package com.pilates.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 회원가입 요청 DTO.
 */
public record SignupRequest(
        @NotBlank(message = "SMS 인증 토큰이 필요합니다.")
        String verifiedToken,

        @NotBlank(message = "이름을 입력해주세요.")
        String name,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        @NotNull(message = "성별을 선택해주세요.")
        String gender,

        /** 생년월일 (선택, YYYY-MM-DD) */
        String birthDate
) {
}
