package com.pilates.common.util;

import java.util.regex.Pattern;

/**
 * 개인정보 마스킹 유틸리티.
 * 로그, API 응답 등에서 민감 정보를 마스킹할 때 사용.
 * DEBUG 모드에서는 마스킹 해제 가능 (application.yml의 app.logging.masking.enabled로 제어).
 */
public final class MaskingUtil {

    private MaskingUtil() {}

    /** 휴대폰 번호 패턴: 010-1234-5678 또는 01012345678 */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(01[016789])-?(\\d{3,4})-?(\\d{4})");

    /** 이메일 패턴 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    /** 주민등록번호 패턴: 990101-1234567 */
    private static final Pattern SSN_PATTERN =
            Pattern.compile("(\\d{6})-?(\\d{7})");

    /** 휴대폰 번호 마스킹: 010-****-5678 */
    public static String maskPhone(String phone) {
        if (phone == null) return null;
        return PHONE_PATTERN.matcher(phone).replaceAll("$1-****-$3");
    }

    /** 이메일 마스킹: ab***@domain.com */
    public static String maskEmail(String email) {
        if (email == null) return null;
        return EMAIL_PATTERN.matcher(email).replaceAll(mr -> {
            String local = mr.group(1);
            String domain = mr.group(2);
            if (local.length() <= 2) return local + "***@" + domain;
            return local.substring(0, 2) + "***@" + domain;
        });
    }

    /** 주민등록번호 마스킹: 990101-******* */
    public static String maskSsn(String ssn) {
        if (ssn == null) return null;
        return SSN_PATTERN.matcher(ssn).replaceAll("$1-*******");
    }

    /** 이름 마스킹: 김*수 (가운데 글자 마스킹) */
    public static String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    /** 문자열 내 모든 개인정보 패턴을 일괄 마스킹 (로그용) */
    public static String maskAll(String text) {
        if (text == null) return null;
        String result = PHONE_PATTERN.matcher(text).replaceAll("$1-****-$3");
        result = EMAIL_PATTERN.matcher(result).replaceAll(mr -> {
            String local = mr.group(1);
            String domain = mr.group(2);
            if (local.length() <= 2) return local + "***@" + domain;
            return local.substring(0, 2) + "***@" + domain;
        });
        result = SSN_PATTERN.matcher(result).replaceAll("$1-*******");
        return result;
    }
}
