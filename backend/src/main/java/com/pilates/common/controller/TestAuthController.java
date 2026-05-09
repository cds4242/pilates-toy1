package com.pilates.common.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * 개발/테스트 전용 엔드포인트.
 * local, local-h2 프로파일에서만 활성화. 운영 환경에서는 비활성.
 */
@RestController
@RequestMapping("/api/test")
@Profile({"local", "local-h2", "test"})
@RequiredArgsConstructor
public class TestAuthController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/auth-required")
    public ApiResponse<Map<String, Object>> authRequired(@LoginMemberAnnotation LoginMember loginMember) {
        return ApiResponse.success(Map.of(
                "memberId", loginMember.memberId(),
                "role", loginMember.role(),
                "message", "인증 성공"
        ));
    }

    /**
     * SMS 인증번호 조회 (개발 전용).
     * 프론트엔드에서 자동 입력에 사용.
     */
    @GetMapping("/sms-code/{phoneNumber}")
    public ApiResponse<Map<String, String>> getSmsCode(@PathVariable String phoneNumber) {
        String code = redisTemplate.opsForValue().get("sms:code:" + phoneNumber);
        return ApiResponse.success(Map.of("code", code != null ? code : ""));
    }
}
