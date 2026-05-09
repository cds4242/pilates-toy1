package com.pilates.common.logging;

import com.pilates.common.util.MaskingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 요청/응답 로깅 필터.
 * 모든 API 요청과 응답을 [REQ]/[RES] 포맷으로 기록한다.
 * 마스킹 설정에 따라 개인정보를 마스킹 처리.
 * Body는 최대 10KB까지 로깅 (초과 시 truncate).
 */
@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_LENGTH = 10 * 1024; // 10KB
    private static final String REQUEST_LOG = "request";

    @Value("${app.logging.masking.enabled:true}")
    private boolean maskingEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 정적 리소스, actuator는 로깅 제외
        String uri = request.getRequestURI();
        if (uri.startsWith("/actuator") || uri.startsWith("/swagger") || uri.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(wrappedRequest);
            logResponse(wrappedRequest, wrappedResponse, duration);
            wrappedResponse.copyBodyToResponse();
        }
    }

    /** 비밀번호, 인증번호 등 민감 필드는 마스킹 설정과 무관하게 항상 제거 */
    private static final java.util.regex.Pattern SENSITIVE_PATTERN =
            java.util.regex.Pattern.compile("\"(password|passwordHash|code|verifiedToken)\"\\s*:\\s*\"[^\"]*\"");

    private void logRequest(ContentCachingRequestWrapper request) {
        String body = getBody(request.getContentAsByteArray());
        body = SENSITIVE_PATTERN.matcher(body).replaceAll("\"$1\":\"***\"");
        String maskedBody = maskingEnabled ? MaskingUtil.maskAll(body) : body;

        log.info("[REQ] {} {} | IP={} | Body={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                maskedBody);
    }

    private void logResponse(ContentCachingRequestWrapper request,
                             ContentCachingResponseWrapper response, long duration) {
        String body = getBody(response.getContentAsByteArray());
        String maskedBody = maskingEnabled ? MaskingUtil.maskAll(body) : body;

        log.info("[RES] {} {} | Status={} | {}ms | Body={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                maskedBody);
    }

    private String getBody(byte[] content) {
        if (content == null || content.length == 0) {
            return "(empty)";
        }
        String body = new String(content, StandardCharsets.UTF_8);
        if (body.length() > MAX_BODY_LENGTH) {
            return body.substring(0, MAX_BODY_LENGTH) + "...(truncated)";
        }
        return body;
    }
}
