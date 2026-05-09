package com.pilates.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SMS 인증번호 검증 요청 DTO.
 */
public record SmsVerifyRequest(
        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        String phoneNumber,

        @NotBlank(message = "인증번호를 입력해주세요.")
        @Size(min = 6, max = 6, message = "인증번호는 6자리입니다.")
        String code
) {
}
