package com.pilates.common.security.jwt;

import com.pilates.common.error.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 토큰 발급·검증 서비스.
 * HS256 서명, jjwt 0.12.x 사용.
 * Access Token(30분), Refresh Token(14일).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_INSTRUCTOR_ID = "instructorId";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration:1800}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration:1209600}") long refreshTokenExpiration) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration * 1000; // 초 → 밀리초
        this.refreshTokenExpiration = refreshTokenExpiration * 1000;
    }

    /**
     * Access Token 생성 (30분).
     * @param userId 사용자 ID (members.id 또는 admins.id)
     * @param role 역할 (MEMBER, INSTRUCTOR, ADMIN, SUPER_ADMIN)
     * @return JWT 문자열
     */
    public String createAccessToken(Long userId, String role) {
        return createAccessToken(userId, role, null);
    }

    /**
     * Access Token 생성 (30분) - instructorId 포함.
     * @param userId 사용자 ID (members.id 또는 admins.id)
     * @param role 역할
     * @param instructorId 강사 ID (강사인 경우만, nullable)
     * @return JWT 문자열
     */
    public String createAccessToken(Long userId, String role, Long instructorId) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_ROLE, role);

        if (instructorId != null) {
            builder.claim(CLAIM_INSTRUCTOR_ID, instructorId);
        }

        return builder
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성 (14일).
     * @param userId 사용자 ID
     * @return JWT 문자열
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 유효성 검증.
     */
    public boolean validateToken(String token) {
        parseClaims(token);
        return true;
    }

    /**
     * 토큰에서 사용자 ID 추출.
     */
    public Long getMemberIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰에서 역할 추출.
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get(CLAIM_ROLE, String.class);
    }

    /**
     * 토큰에서 강사 ID 추출 (nullable).
     */
    public Long getInstructorIdFromToken(String token) {
        Claims claims = parseClaims(token);
        Object value = claims.get(CLAIM_INSTRUCTOR_ID);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    /**
     * 토큰 타입(access/refresh) 확인.
     */
    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get(CLAIM_TYPE, String.class);
    }

    /**
     * Access Token 만료 시간(초) 반환.
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException(ErrorCode.AUTH_TOKEN_EXPIRED, e);
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException(ErrorCode.AUTH_TOKEN_MALFORMED, e);
        } catch (Exception e) {
            throw new JwtAuthenticationException(ErrorCode.AUTH_TOKEN_INVALID, e);
        }
    }
}
