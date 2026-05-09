package com.pilates.common.security;

import com.pilates.common.security.jwt.JwtAuthenticationException;
import com.pilates.common.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 토큰 발급·검증 테스트.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Base64 인코딩된 32바이트 키
        jwtTokenProvider = new JwtTokenProvider(
                "dGVzdC1qd3Qtc2VjcmV0LWtleS0zMi1ieXRlcyEhISE=",
                1800,   // 30분
                1209600 // 14일
        );
    }

    @Test
    @DisplayName("Access Token 생성 및 검증")
    void createAndValidateAccessToken() {
        String token = jwtTokenProvider.createAccessToken(1L, "MEMBER");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getMemberIdFromToken(token)).isEqualTo(1L);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo("MEMBER");
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("access");
    }

    @Test
    @DisplayName("Refresh Token 생성 및 검증")
    void createAndValidateRefreshToken() {
        String token = jwtTokenProvider.createRefreshToken(42L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getMemberIdFromToken(token)).isEqualTo(42L);
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("잘못된 토큰은 예외를 발생시킨다")
    void invalidTokenThrows() {
        assertThatThrownBy(() -> jwtTokenProvider.validateToken("invalid.token.here"))
                .isInstanceOf(JwtAuthenticationException.class);
    }

    @Test
    @DisplayName("만료된 토큰은 AUTH_TOKEN_EXPIRED 예외를 발생시킨다")
    void expiredTokenThrows() {
        // 만료 시간을 0초로 설정한 프로바이더
        JwtTokenProvider expiredProvider = new JwtTokenProvider(
                "dGVzdC1qd3Qtc2VjcmV0LWtleS0zMi1ieXRlcyEhISE=",
                0, 0
        );
        String token = expiredProvider.createAccessToken(1L, "MEMBER");

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                .isInstanceOf(JwtAuthenticationException.class);
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 검증 실패한다")
    void wrongKeyThrows() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "YW5vdGhlci1rZXktMzItYnl0ZXMtbG9uZy1leCEhISE=",
                1800, 1209600
        );
        String token = otherProvider.createAccessToken(1L, "MEMBER");

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                .isInstanceOf(JwtAuthenticationException.class);
    }
}
