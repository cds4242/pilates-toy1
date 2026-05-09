package com.pilates.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "SMS 인증번호 발송 요청")
public record SmsRequest(
        @Schema(description = "휴대폰 번호", example = "010-1234-5678")
        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        String phoneNumber
) {
}
