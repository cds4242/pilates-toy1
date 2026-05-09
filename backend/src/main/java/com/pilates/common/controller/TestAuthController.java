package com.pilates.common.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * 인증 검증용 임시 엔드포인트.
 * local, local-h2 프로파일에서만 활성화. 운영 환경에서는 비활성.
 */
@RestController
@RequestMapping("/api/test")
@Profile({"local", "local-h2", "test"})
public class TestAuthController {

    /**
     * 인증이 필요한 테스트 엔드포인트.
     * 유효한 JWT Access Token이 있어야 200 응답.
     */
    @GetMapping("/auth-required")
    public ApiResponse<Map<String, Object>> authRequired(@LoginMemberAnnotation LoginMember loginMember) {
        return ApiResponse.success(Map.of(
                "memberId", loginMember.memberId(),
                "role", loginMember.role(),
                "message", "인증 성공"
        ));
    }
}
