package com.pilates.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 재설정 요청")
public record PasswordResetRequest(
        @Schema(description = "SMS 인증 토큰", example = "cd66cba218b34505a753187f8a4aa764")
        @NotBlank(message = "SMS 인증 토큰이 필요합니다.")
        String verifiedToken,

        @Schema(description = "새 비밀번호 (8자 이상, 3종 이상 문자)", example = "NewPass123!")
        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        String newPassword
) {
}
