package com.pilates.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 공통 API 응답 래퍼.
 * 모든 API는 이 포맷으로 응답한다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;
    private final LocalDateTime timestamp;

    /** 성공 응답 (데이터 포함) */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    /** 성공 응답 (데이터 없음) */
    public static <Void> ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null, LocalDateTime.now());
    }

    /** 실패 응답 */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message), LocalDateTime.now());
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorDetail {
        private final String code;
        private final String message;
    }
}
