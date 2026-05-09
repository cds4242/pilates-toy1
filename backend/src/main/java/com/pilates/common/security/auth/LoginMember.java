package com.pilates.common.security.auth;

/**
 * 현재 로그인한 회원 정보.
 * JwtAuthenticationFilter에서 추출한 정보를 담아 Controller에 전달한다.
 */
public record LoginMember(
        Long memberId,
        String role
) {
}
