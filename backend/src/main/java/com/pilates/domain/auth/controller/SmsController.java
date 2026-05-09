package com.pilates.domain.auth.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.hash.PhoneNumberNormalizer;
import com.pilates.domain.auth.dto.SmsRequest;
import com.pilates.domain.auth.dto.SmsVerifyRequest;
import com.pilates.domain.auth.dto.SmsVerifyResponse;
import com.pilates.domain.auth.service.SmsVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SMS 인증 API.
 */
@Tag(name = "Auth", description = "인증 API (회원가입, 로그인, 토큰 갱신, 비밀번호 재설정)")
@RestController
@RequestMapping("/api/auth/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsVerificationService smsVerificationService;
    private final PhoneNumberNormalizer phoneNumberNormalizer;

    @Operation(summary = "SMS 인증번호 발송", description = "휴대폰 번호로 6자리 인증번호를 발송한다. 1분 내 재발송 불가, 일 5회 제한.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "올바르지 않은 휴대폰 번호"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "발송 제한 초과")
    })
    @PostMapping("/request")
    public ApiResponse<Void> requestCode(@Valid @RequestBody SmsRequest request) {
        String normalized = phoneNumberNormalizer.normalize(request.phoneNumber());
        smsVerificationService.sendVerificationCode(normalized);
        return ApiResponse.success();
    }

    @Operation(summary = "SMS 인증번호 검증", description = "발송된 인증번호를 검증한다. 성공 시 회원가입/비밀번호 재설정에 사용할 verifiedToken 반환.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공, verifiedToken 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증번호 불일치 또는 만료")
    })
    @PostMapping("/verify")
    public ApiResponse<SmsVerifyResponse> verifyCode(@Valid @RequestBody SmsVerifyRequest request) {
        String normalized = phoneNumberNormalizer.normalize(request.phoneNumber());
        String verifiedToken = smsVerificationService.verifyCode(normalized, request.code());
        return ApiResponse.success(new SmsVerifyResponse(verifiedToken));
    }
}
