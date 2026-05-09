package com.pilates.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "SMS 인증 토큰 (32자 UUID)", example = "cd66cba218b34505a753187f8a4aa764")
        @NotBlank(message = "SMS 인증 토큰이 필요합니다.")
        String verifiedToken,

        @Schema(description = "회원 이름", example = "김민지")
        @NotBlank(message = "이름을 입력해주세요.")
        String name,

        @Schema(description = "비밀번호 (8자 이상, 대소문자·숫자·특수문자 중 3종)", example = "Test1234!")
        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        @Schema(description = "성별 (MALE/FEMALE)", example = "FEMALE")
        @NotNull(message = "성별을 선택해주세요.")
        String gender,

        @Schema(description = "생년월일 (선택, YYYY-MM-DD)", example = "1995-03-15")
        String birthDate
) {
}
