package com.pilates.common.security;

import com.pilates.common.error.BusinessException;
import com.pilates.common.security.password.PasswordPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 비밀번호 정책 테스트.
 */
class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @ParameterizedTest
    @ValueSource(strings = {
            "Abc12345!",       // 대소문자+숫자+특수
            "abcdef1!",        // 소문자+숫자+특수
            "ABCDEF1!",        // 대문자+숫자+특수
            "ABCDabcd1"        // 대문자+소문자+숫자
    })
    @DisplayName("유효한 비밀번호는 통과한다")
    void validPasswords(String password) {
        assertThatCode(() -> policy.validate(password))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",             // 너무 짧음
            "abcdefgh",        // 소문자만 (1종)
            "12345678",        // 숫자만 (1종)
            "abcd1234",        // 소문자+숫자 (2종)
            "ABCD1234"         // 대문자+숫자 (2종)
    })
    @DisplayName("정책 위반 비밀번호는 예외를 발생시킨다")
    void invalidPasswords(String password) {
        assertThatThrownBy(() -> policy.validate(password))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("null 비밀번호는 예외를 발생시킨다")
    void nullPassword() {
        assertThatThrownBy(() -> policy.validate(null))
                .isInstanceOf(BusinessException.class);
    }
}
