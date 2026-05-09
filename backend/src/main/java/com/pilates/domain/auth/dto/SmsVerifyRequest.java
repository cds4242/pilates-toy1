package com.pilates.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "SMS 인증번호 검증 요청")
public record SmsVerifyRequest(
        @Schema(description = "휴대폰 번호", example = "010-1234-5678")
        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        String phoneNumber,

        @Schema(description = "6자리 인증번호", example = "123456")
        @NotBlank(message = "인증번호를 입력해주세요.")
        @Size(min = 6, max = 6, message = "인증번호는 6자리입니다.")
        String code
) {
}
