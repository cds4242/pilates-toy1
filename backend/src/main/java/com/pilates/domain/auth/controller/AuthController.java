package com.pilates.domain.auth.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.auth.dto.*;
import com.pilates.domain.auth.service.AuthService;
import com.pilates.domain.member.dto.PasswordResetRequest;
import com.pilates.domain.member.service.MemberService;
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
 * 회원 인증 API.
 * /api/auth/** 경로는 SecurityConfig에서 permitAll.
 */
@Tag(name = "Auth", description = "인증 API (회원가입, 로그인, 토큰 갱신, 비밀번호 재설정)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;

    @Operation(summary = "회원가입", description = "SMS 인증 완료 후 verifiedToken으로 회원가입. 가입 성공 시 Access/Refresh 토큰 발급.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 비밀번호 정책 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가입된 휴대폰 번호")
    })
    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(request));
    }

    @Operation(summary = "로그인", description = "휴대폰 번호 + 비밀번호로 로그인. Access/Refresh 토큰 발급.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "번호 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access/Refresh 토큰 발급 (Rotation 방식).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "갱신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 만료 또는 재사용 감지")
    })
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @Operation(summary = "비밀번호 재설정", description = "SMS 인증 완료 후 verifiedToken으로 비밀번호 변경. 로그인 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "SMS 인증 만료 또는 비밀번호 정책 위반")
    })
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        memberService.resetPassword(request);
        return ApiResponse.success();
    }
}
