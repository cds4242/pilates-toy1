package com.pilates.common.security.hash;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 해시 서비스.
 * 주로 휴대폰 번호 해시(phone_hash)에 사용. 검색/중복 확인용.
 * hex 문자열 64자 반환.
 */
@Component
public class HashingService {

    private static final String ALGORITHM = "SHA-256";

    /**
     * 입력값을 SHA-256으로 해시한다.
     * @param input 해시할 문자열
     * @return 64자 hex 문자열
     */
    public String hash(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("해시 대상이 비어있습니다.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
