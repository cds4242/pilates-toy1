package com.pilates.config.security;

import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberArgumentResolver;
import com.pilates.common.security.jwt.JwtAuthenticationException;
import com.pilates.common.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터.
 * Authorization 헤더에서 Bearer 토큰을 추출하여 검증하고,
 * SecurityContext와 request attribute에 인증 정보를 설정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                jwtTokenProvider.validateToken(token);
                String tokenType = jwtTokenProvider.getTokenType(token);

                // Refresh 토큰은 인증 필터에서 사용하지 않음
                if ("access".equals(tokenType)) {
                    Long memberId = jwtTokenProvider.getMemberIdFromToken(token);
                    String role = jwtTokenProvider.getRoleFromToken(token);

                    // SecurityContext 설정
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var authentication = new UsernamePasswordAuthenticationToken(memberId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // @LoginMember 리졸버용 attribute 설정
                    request.setAttribute(LoginMemberArgumentResolver.LOGIN_MEMBER_ATTRIBUTE,
                            new LoginMember(memberId, role));
                }
            } catch (JwtAuthenticationException e) {
                log.debug("JWT 인증 실패: {}", e.getMessage());
                // 인증 실패 시 SecurityContext를 설정하지 않음 → Spring Security가 401 처리
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
