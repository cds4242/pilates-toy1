package com.pilates.common.security.auth;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @LoginMember 어노테이션이 붙은 파라미터에 LoginMember를 주입하는 리졸버.
 * JwtAuthenticationFilter에서 request attribute에 저장한 값을 꺼낸다.
 */
@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String LOGIN_MEMBER_ATTRIBUTE = "loginMember";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMemberAnnotation.class)
                && parameter.getParameterType().equals(LoginMember.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        LoginMember loginMember = (LoginMember) request.getAttribute(LOGIN_MEMBER_ATTRIBUTE);
        if (loginMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return loginMember;
    }
}
