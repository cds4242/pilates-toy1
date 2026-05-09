package com.pilates.domain.auth.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.common.security.hash.HashingService;
import com.pilates.common.security.hash.PhoneNumberNormalizer;
import com.pilates.common.security.jwt.JwtTokenProvider;
import com.pilates.common.security.password.PasswordPolicy;
import com.pilates.domain.auth.dto.*;
import com.pilates.domain.member.entity.Gender;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.entity.MemberStatus;
import com.pilates.domain.member.repository.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * 인증 서비스.
 * 회원가입, 로그인, 토큰 갱신을 담당한다.
 *
 * 보안 원칙:
 * - 비밀번호는 BCrypt 해시만 저장, 평문은 메모리에서 즉시 사용 후 폐기
 * - 휴대폰 번호: 정규화 → SHA-256 해시(검색용) + AES 암호화(표시용)
 * - 이름, 생년월일: AES 암호화
 * - Refresh Token: Redis에 저장, rotation 방식 (갱신 시 이전 토큰 무효화)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ROLE_MEMBER = "MEMBER";
    private static final String REFRESH_TOKEN_KEY = "auth:refresh:";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    private final MemberRepository memberRepository;
    private final SmsVerificationService smsVerificationService;
    private final EncryptionService encryptionService;
    private final HashingService hashingService;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    /**
     * 회원가입.
     * SMS 인증 → 중복 검사 → 비밀번호 해시 → 개인정보 암호화 → 저장 → 토큰 발급
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 1. SMS 인증 토큰에서 전화번호 추출 (1회용, 즉시 무효화)
        String phoneNumber = smsVerificationService.getVerifiedPhoneNumber(request.verifiedToken());

        // 2. 비밀번호 정책 검증
        passwordPolicy.validate(request.password());

        // 3. 전화번호 해시 생성 (중복 검사 + DB 저장용)
        String phoneHash = hashingService.hash(phoneNumber);

        // 4. 중복 가입 검사 (phone_hash UNIQUE)
        if (memberRepository.existsByPhoneHashAndDeletedAtIsNull(phoneHash)) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        // 5. 개인정보 암호화
        String encryptedName = encryptionService.encrypt(request.name());
        String encryptedPhone = encryptionService.encrypt(phoneNumber);
        String encryptedBirth = request.birthDate() != null
                ? encryptionService.encrypt(request.birthDate()) : null;

        // 6. 비밀번호 해시 (BCrypt, strength 12)
        String hashedPassword = passwordEncoder.encode(request.password());

        // 7. 회원 엔티티 생성
        Member member = Member.builder()
                .publicId(UUID.randomUUID().toString().replace("-", ""))
                .name(encryptedName)
                .phoneEncrypted(encryptedPhone)
                .phoneHash(phoneHash)
                .birthEncrypted(encryptedBirth)
                .gender(Gender.valueOf(request.gender().toUpperCase()))
                .status(MemberStatus.ACTIVE)
                .passwordHash(hashedPassword)
                .build();

        try {
            memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            // 동시 가입 시 phone_hash UNIQUE 제약 위반
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        // 8. 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), ROLE_MEMBER);
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // 9. Refresh Token Redis에 저장
        saveRefreshToken(member.getId(), refreshToken);

        log.info("회원가입 완료: publicId={}", member.getPublicId());

        return new SignupResponse(
                member.getPublicId(),
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    /**
     * 로그인.
     * 전화번호 해시 → 회원 조회 → 비밀번호 검증 → 토큰 발급
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // 1. 전화번호 정규화 + 해시
        String normalized = phoneNumberNormalizer.normalize(request.phoneNumber());
        String phoneHash = hashingService.hash(normalized);

        // 2. 회원 조회
        Member member = memberRepository.findByPhoneHashAndDeletedAtIsNull(phoneHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_LOGIN_FAILED));

        // 3. 상태 확인
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        // 5. 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), ROLE_MEMBER);
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // 6. Refresh Token 저장 (이전 토큰 자동 교체)
        saveRefreshToken(member.getId(), refreshToken);

        log.info("로그인 성공: memberId={}", member.getId());

        return new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirationSeconds());
    }

    /**
     * 토큰 갱신 (Refresh Token Rotation).
     * 기존 Refresh Token 검증 → 새 Access + Refresh 발급 → 기존 Refresh 무효화
     */
    public TokenResponse refresh(TokenRefreshRequest request) {
        String oldRefreshToken = request.refreshToken();

        // 1. Refresh Token 검증 (서명 + 만료)
        jwtTokenProvider.validateToken(oldRefreshToken);

        // 2. 토큰 타입 확인
        String tokenType = jwtTokenProvider.getTokenType(oldRefreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        // 3. 회원 ID 추출
        Long memberId = jwtTokenProvider.getMemberIdFromToken(oldRefreshToken);

        // 4. Redis에 저장된 Refresh Token과 비교 (Rotation 검증)
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + memberId);
        if (storedToken == null || !storedToken.equals(oldRefreshToken)) {
            // 토큰 재사용 감지 → 해당 회원의 모든 세션 무효화
            redisTemplate.delete(REFRESH_TOKEN_KEY + memberId);
            log.warn("Refresh Token 재사용 감지: memberId={}", memberId);
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        // 5. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(memberId, ROLE_MEMBER);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);

        // 6. 새 Refresh Token으로 교체 (이전 것 자동 무효화)
        saveRefreshToken(memberId, newRefreshToken);

        log.info("토큰 갱신: memberId={}", memberId);

        return new TokenResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenExpirationSeconds());
    }

    /**
     * Refresh Token을 Redis에 저장한다.
     * 같은 memberId에 대해 1개만 유지 (이전 토큰 자동 교체).
     */
    private void saveRefreshToken(Long memberId, String refreshToken) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY + memberId, refreshToken, REFRESH_TOKEN_TTL);
    }
}
