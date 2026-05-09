package com.pilates.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 요청 DTO.
 * SMS 인증 완료 후 사용.
 */
public record PasswordResetRequest(
        @NotBlank(message = "SMS 인증 토큰이 필요합니다.")
        String verifiedToken,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        String newPassword
) {
}
