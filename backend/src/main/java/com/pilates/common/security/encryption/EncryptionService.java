package com.pilates.common.security.encryption;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256/GCM 암호화 서비스.
 * IV(12바이트)를 매번 랜덤 생성하여 같은 평문도 다른 암호문을 만든다.
 * 암호문 형식: "{keyVersion}::{base64(iv + ciphertext + tag)}"
 */
@Slf4j
@Component
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final String VERSION_SEPARATOR = "::";

    private final SecretKeySpec secretKey;
    private final String keyVersion;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(EncryptionKeyProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.getKey());
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 키는 32바이트여야 합니다. 현재: " + keyBytes.length + "바이트");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.keyVersion = properties.getKeyVersion();
    }

    /**
     * 평문을 AES-256/GCM으로 암호화한다.
     * @param plaintext 암호화할 평문
     * @return "{keyVersion}::{base64(iv + ciphertext + tag)}" 형식의 암호문
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // iv + ciphertext(+tag) 결합
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return keyVersion + VERSION_SEPARATOR + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("암호화 실패", e);
            throw new BusinessException(ErrorCode.ENCRYPTION_FAILED);
        }
    }

    /**
     * 암호문을 복호화한다.
     * @param encrypted "{keyVersion}::{base64}" 형식의 암호문
     * @return 복호화된 평문
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return encrypted;
        }
        try {
            int separatorIndex = encrypted.indexOf(VERSION_SEPARATOR);
            if (separatorIndex < 0) {
                throw new BusinessException(ErrorCode.ENCRYPTION_KEY_VERSION_MISMATCH);
            }

            String version = encrypted.substring(0, separatorIndex);
            if (!keyVersion.equals(version)) {
                throw new BusinessException(ErrorCode.ENCRYPTION_KEY_VERSION_MISMATCH);
            }

            byte[] combined = Base64.getDecoder().decode(encrypted.substring(separatorIndex + VERSION_SEPARATOR.length()));

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("복호화 실패", e);
            throw new BusinessException(ErrorCode.ENCRYPTION_FAILED);
        }
    }
}
