package com.pilates.domain.admin.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.admin.dto.AdminLoginRequest;
import com.pilates.domain.admin.dto.AdminLoginResponse;
import com.pilates.domain.admin.service.AdminAuthService;
import com.pilates.domain.auth.dto.TokenRefreshRequest;
import com.pilates.domain.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Auth", description = "관리자·강사 인증 API")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "관리자 로그인", description = "관리자/강사 계정으로 로그인한다.")
    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminAuthService.login(request));
    }

    @Operation(summary = "관리자 토큰 갱신")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(adminAuthService.refresh(request));
    }

    @Operation(summary = "관리자 로그아웃")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@LoginMemberAnnotation LoginMember loginMember) {
        adminAuthService.logout(loginMember.memberId());
        return ApiResponse.success();
    }
}
