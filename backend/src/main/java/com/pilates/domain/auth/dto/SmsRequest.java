package com.pilates.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * SMS 인증번호 요청 DTO.
 */
public record SmsRequest(
        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        String phoneNumber
) {
}
