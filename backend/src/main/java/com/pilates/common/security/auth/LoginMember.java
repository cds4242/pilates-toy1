package com.pilates.common.security.auth;

/**
 * 현재 로그인한 사용자 정보.
 * JwtAuthenticationFilter에서 추출한 정보를 담아 Controller에 전달한다.
 *
 * - 회원 로그인: memberId = members.id, role = "MEMBER", instructorId = null
 * - 강사 로그인: memberId = admins.id, role = "INSTRUCTOR", instructorId = instructors.id
 * - 관리자 로그인: memberId = admins.id, role = "ADMIN"/"SUPER_ADMIN", instructorId = null
 */
public record LoginMember(
        Long memberId,
        String role,
        Long instructorId
) {
    /** 회원용 생성자 (하위 호환) */
    public LoginMember(Long memberId, String role) {
        this(memberId, role, null);
    }
}
