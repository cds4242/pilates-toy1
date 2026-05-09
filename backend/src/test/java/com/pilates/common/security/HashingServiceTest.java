package com.pilates.common.security;

import com.pilates.common.security.hash.HashingService;
import com.pilates.common.security.hash.PhoneNumberNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SHA-256 해시 + 전화번호 정규화 테스트.
 */
class HashingServiceTest {

    private final HashingService hashingService = new HashingService();
    private final PhoneNumberNormalizer normalizer = new PhoneNumberNormalizer();

    @Test
    @DisplayName("SHA-256 해시는 64자 hex 문자열이다")
    void hashLength() {
        String hash = hashingService.hash("01012345678");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("같은 입력은 같은 해시를 생성한다")
    void sameInputSameHash() {
        String hash1 = hashingService.hash("01012345678");
        String hash2 = hashingService.hash("01012345678");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("다른 형식의 같은 번호는 정규화 후 같은 해시를 생성한다")
    void differentFormatSameHash() {
        String normalized1 = normalizer.normalize("010-1234-5678");
        String normalized2 = normalizer.normalize("01012345678");
        String normalized3 = normalizer.normalize("+82-10-1234-5678");

        assertThat(normalized1).isEqualTo(normalized2).isEqualTo(normalized3);

        String hash1 = hashingService.hash(normalized1);
        String hash2 = hashingService.hash(normalized2);
        String hash3 = hashingService.hash(normalized3);
        assertThat(hash1).isEqualTo(hash2).isEqualTo(hash3);
    }

    @Test
    @DisplayName("전화번호 정규화: 하이픈 제거")
    void normalizeRemovesHyphens() {
        assertThat(normalizer.normalize("010-1234-5678")).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("전화번호 정규화: +82 국가번호 처리")
    void normalizeHandlesCountryCode() {
        assertThat(normalizer.normalize("+821012345678")).isEqualTo("01012345678");
    }
}
