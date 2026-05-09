package com.pilates.domain.admin.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.security.jwt.JwtTokenProvider;
import com.pilates.domain.admin.dto.AdminLoginRequest;
import com.pilates.domain.admin.dto.AdminLoginResponse;
import com.pilates.domain.admin.entity.Admin;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.auth.dto.TokenRefreshRequest;
import com.pilates.domain.auth.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final String REFRESH_TOKEN_KEY = "auth:admin:refresh:";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByLoginIdAndDeletedAtIsNull(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        if (!admin.isActive()) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        Long instructorId = admin.getInstructor() != null ? admin.getInstructor().getId() : null;
        String role = admin.getRole().name();

        String accessToken = jwtTokenProvider.createAccessToken(admin.getId(), role, instructorId);
        String refreshToken = jwtTokenProvider.createRefreshToken(admin.getId());

        redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY + admin.getId(), refreshToken, REFRESH_TOKEN_TTL);

        admin.updateLastLoginAt();

        log.info("관리자 로그인: adminId={}, role={}, instructorId={}", admin.getId(), role, instructorId);

        return new AdminLoginResponse(
                admin.getId(), role, instructorId,
                accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    public TokenResponse refresh(TokenRefreshRequest request) {
        String oldRefreshToken = request.refreshToken();

        jwtTokenProvider.validateToken(oldRefreshToken);

        String tokenType = jwtTokenProvider.getTokenType(oldRefreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        Long adminId = jwtTokenProvider.getMemberIdFromToken(oldRefreshToken);

        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + adminId);
        if (storedToken == null || !storedToken.equals(oldRefreshToken)) {
            redisTemplate.delete(REFRESH_TOKEN_KEY + adminId);
            log.warn("Admin Refresh Token 재사용 감지: adminId={}", adminId);
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        Long instructorId = admin.getInstructor() != null ? admin.getInstructor().getId() : null;
        String role = admin.getRole().name();

        String newAccessToken = jwtTokenProvider.createAccessToken(adminId, role, instructorId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(adminId);

        redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY + adminId, newRefreshToken, REFRESH_TOKEN_TTL);

        return new TokenResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenExpirationSeconds());
    }

    public void logout(Long adminId) {
        redisTemplate.delete(REFRESH_TOKEN_KEY + adminId);
        log.info("관리자 로그아웃: adminId={}", adminId);
    }
}
