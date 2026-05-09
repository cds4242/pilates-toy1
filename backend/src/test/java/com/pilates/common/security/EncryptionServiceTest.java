package com.pilates.common.security;

import com.pilates.common.error.BusinessException;
import com.pilates.common.security.encryption.EncryptionKeyProperties;
import com.pilates.common.security.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AES-256/GCM 암호화 서비스 테스트.
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        EncryptionKeyProperties properties = new EncryptionKeyProperties();
        properties.setKey("dGVzdC1rZXktMzItYnl0ZXMtbG9uZy1leGFtcGxlISE="); // 32바이트
        properties.setKeyVersion("v1");
        encryptionService = new EncryptionService(properties);
    }

    @Test
    @DisplayName("암호화 후 복호화하면 원본과 일치한다")
    void encryptThenDecrypt() {
        String plaintext = "김민지";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("같은 평문을 암호화해도 매번 다른 암호문이 생성된다 (랜덤 IV)")
    void samePlaintextDifferentCiphertext() {
        String plaintext = "01012345678";
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // 둘 다 복호화하면 동일
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("암호문에 버전 prefix가 포함된다")
    void encryptedHasVersionPrefix() {
        String encrypted = encryptionService.encrypt("테스트");
        assertThat(encrypted).startsWith("v1::");
    }

    @Test
    @DisplayName("null/빈 문자열은 그대로 반환한다")
    void nullAndBlankPassThrough() {
        assertThat(encryptionService.encrypt(null)).isNull();
        assertThat(encryptionService.encrypt("")).isEmpty();
        assertThat(encryptionService.decrypt(null)).isNull();
        assertThat(encryptionService.decrypt("")).isEmpty();
    }

    @Test
    @DisplayName("버전 불일치 시 예외가 발생한다")
    void versionMismatchThrows() {
        String encrypted = encryptionService.encrypt("테스트");
        String tampered = "v2::" + encrypted.substring(4); // 버전을 v2로 변경

        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
                .isInstanceOf(BusinessException.class);
    }
}
