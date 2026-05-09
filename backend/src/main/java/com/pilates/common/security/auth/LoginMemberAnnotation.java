package com.pilates.common.security.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 메서드 파라미터에 현재 로그인 회원 정보를 주입하는 어노테이션.
 * <pre>
 * {@code
 * @GetMapping("/me")
 * public ApiResponse<MeResponse> getMe(@LoginMember LoginMember loginMember) { ... }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMemberAnnotation {
}
