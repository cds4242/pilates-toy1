package com.pilates.common.security.password;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 정책 검증.
 * 최소 8자, 대소문자·숫자·특수문자 중 3종 이상 포함.
 */
@Component
public class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int REQUIRED_CATEGORIES = 3;

    /**
     * 비밀번호 정책 검증. 위반 시 예외 발생.
     * @param password 검증할 비밀번호 (평문)
     */
    public void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_POLICY_VIOLATION);
        }

        int categories = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) categories++;
        if (password.chars().anyMatch(Character::isLowerCase)) categories++;
        if (password.chars().anyMatch(Character::isDigit)) categories++;
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) categories++;

        if (categories < REQUIRED_CATEGORIES) {
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_POLICY_VIOLATION);
        }
    }
}
