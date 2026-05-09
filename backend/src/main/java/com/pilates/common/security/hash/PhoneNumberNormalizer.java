package com.pilates.common.security.hash;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 휴대폰 번호 정규화.
 * 다양한 입력 형식(010-1234-5678, 01012345678, +82-10-1234-5678)을
 * 통일된 형식(01012345678)으로 변환한다. 해시 시 동일한 번호가 같은 해시를 생성하도록 보장.
 */
@Component
public class PhoneNumberNormalizer {

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");
    private static final Pattern VALID_PHONE = Pattern.compile("^010\\d{8}$");

    /**
     * 휴대폰 번호를 정규화한다.
     * @param phoneNumber 입력 번호 (하이픈, 공백, +82 등 허용)
     * @return 11자리 숫자 (예: "01012345678")
     */
    public String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER);
        }

        // 숫자만 추출
        String digits = phoneNumber.replaceAll("[^0-9]", "");

        // +82 국가번호 처리 (8210... → 010...)
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }

        // 유효성 검증
        if (!VALID_PHONE.matcher(digits).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER);
        }

        return digits;
    }
}
