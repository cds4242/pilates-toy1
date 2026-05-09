package com.pilates.domain.auth.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * SMS 인증번호 발송·검증 서비스.
 * Redis에 인증번호와 Rate Limit 카운터를 저장한다.
 *
 * Redis 키 구조:
 * - sms:code:{phone}       → 인증번호 (TTL 5분)
 * - sms:rate:{phone}       → 1분 내 발송 카운터 (TTL 60초)
 * - sms:daily:{phone}      → 일일 발송 카운터 (TTL 24시간)
 * - sms:verified:{token}   → 인증 완료된 전화번호 (TTL 10분)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT_TTL = Duration.ofSeconds(60);
    private static final Duration DAILY_LIMIT_TTL = Duration.ofHours(24);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);
    private static final int DAILY_LIMIT = 5;

    private static final String KEY_CODE = "sms:code:";
    private static final String KEY_RATE = "sms:rate:";
    private static final String KEY_DAILY = "sms:daily:";
    private static final String KEY_VERIFIED = "sms:verified:";

    private final StringRedisTemplate redisTemplate;
    private final SmsService smsService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 인증번호를 생성하여 SMS로 발송한다.
     * @param phoneNumber 정규화된 전화번호 (01012345678)
     */
    public void sendVerificationCode(String phoneNumber) {
        checkRateLimit(phoneNumber);

        String code = generateCode();

        // Redis에 인증번호 저장 (기존 코드 덮어쓰기)
        redisTemplate.opsForValue().set(KEY_CODE + phoneNumber, code, CODE_TTL);

        // 1분 Rate Limit 카운터 설정
        redisTemplate.opsForValue().set(KEY_RATE + phoneNumber, "1", RATE_LIMIT_TTL);

        // 일일 카운터 증가
        String dailyKey = KEY_DAILY + phoneNumber;
        Long count = redisTemplate.opsForValue().increment(dailyKey);
        if (count != null && count == 1) {
            redisTemplate.expire(dailyKey, DAILY_LIMIT_TTL);
        }

        // SMS 발송
        smsService.send(phoneNumber, "[필라테스] 인증번호: " + code + " (5분 이내 입력)");
    }

    /**
     * 인증번호를 검증한다.
     * @param phoneNumber 정규화된 전화번호
     * @param code 사용자가 입력한 인증번호
     * @return 인증 성공 시 verifiedToken (UUID), 회원가입 API에서 사용
     */
    public String verifyCode(String phoneNumber, String code) {
        String storedCode = redisTemplate.opsForValue().get(KEY_CODE + phoneNumber);

        if (storedCode == null) {
            throw new BusinessException(ErrorCode.SMS_CODE_EXPIRED);
        }

        if (!storedCode.equals(code)) {
            throw new BusinessException(ErrorCode.SMS_CODE_MISMATCH);
        }

        // 인증 성공: 코드 삭제 + verifiedToken 발급
        redisTemplate.delete(KEY_CODE + phoneNumber);

        String verifiedToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_VERIFIED + verifiedToken, phoneNumber, VERIFIED_TTL);

        return verifiedToken;
    }

    /**
     * verifiedToken으로 인증된 전화번호를 조회한다.
     * 조회 성공 시 토큰을 삭제한다 (1회용).
     * @param verifiedToken 인증 토큰
     * @return 인증된 전화번호
     */
    public String getVerifiedPhoneNumber(String verifiedToken) {
        String phoneNumber = redisTemplate.opsForValue().get(KEY_VERIFIED + verifiedToken);

        if (phoneNumber == null) {
            throw new BusinessException(ErrorCode.SMS_VERIFICATION_REQUIRED);
        }

        redisTemplate.delete(KEY_VERIFIED + verifiedToken);
        return phoneNumber;
    }

    private void checkRateLimit(String phoneNumber) {
        // 1분 내 재발송 제한
        String rateKey = KEY_RATE + phoneNumber;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            throw new BusinessException(ErrorCode.SMS_RATE_LIMIT_EXCEEDED);
        }

        // 일일 발송 제한
        String dailyKey = KEY_DAILY + phoneNumber;
        String dailyCount = redisTemplate.opsForValue().get(dailyKey);
        if (dailyCount != null && Integer.parseInt(dailyCount) >= DAILY_LIMIT) {
            throw new BusinessException(ErrorCode.SMS_DAILY_LIMIT_EXCEEDED);
        }
    }

    private String generateCode() {
        int code = secureRandom.nextInt(900000) + 100000; // 100000~999999
        return String.valueOf(code);
    }
}
