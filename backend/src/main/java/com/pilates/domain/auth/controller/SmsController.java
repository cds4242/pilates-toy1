package com.pilates.domain.auth.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.hash.PhoneNumberNormalizer;
import com.pilates.domain.auth.dto.SmsRequest;
import com.pilates.domain.auth.dto.SmsVerifyRequest;
import com.pilates.domain.auth.dto.SmsVerifyResponse;
import com.pilates.domain.auth.service.SmsVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SMS 인증 API.
 * /api/auth/** 경로는 SecurityConfig에서 permitAll.
 */
@RestController
@RequestMapping("/api/auth/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsVerificationService smsVerificationService;
    private final PhoneNumberNormalizer phoneNumberNormalizer;

    /**
     * 인증번호 발송 요청.
     * POST /api/auth/sms/request
     */
    @PostMapping("/request")
    public ApiResponse<Void> requestCode(@Valid @RequestBody SmsRequest request) {
        String normalized = phoneNumberNormalizer.normalize(request.phoneNumber());
        smsVerificationService.sendVerificationCode(normalized);
        return ApiResponse.success();
    }

    /**
     * 인증번호 검증.
     * POST /api/auth/sms/verify
     */
    @PostMapping("/verify")
    public ApiResponse<SmsVerifyResponse> verifyCode(@Valid @RequestBody SmsVerifyRequest request) {
        String normalized = phoneNumberNormalizer.normalize(request.phoneNumber());
        String verifiedToken = smsVerificationService.verifyCode(normalized, request.code());
        return ApiResponse.success(new SmsVerifyResponse(verifiedToken));
    }
}
