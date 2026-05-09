package com.pilates.domain.payment.controller;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.response.ApiResponse;
import com.pilates.domain.payment.dto.TossWebhookPayload;
import com.pilates.domain.payment.service.TossWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 토스 웹훅 수신 컨트롤러.
 * /api/webhooks/toss는 permitAll (시그니처로 인증).
 */
@Slf4j
@Tag(name = "Toss Webhook", description = "토스페이먼츠 웹훅 (시그니처 검증)")
@RestController
@RequestMapping("/api/webhooks/toss")
@RequiredArgsConstructor
public class TossWebhookController {

    private final TossWebhookService webhookService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Operation(summary = "토스 웹훅 수신", description = "토스에서 결제 상태 변경 시 호출. HMAC-SHA256 시그니처 검증 필수.")
    @PostMapping
    public ApiResponse<Void> handleWebhook(
            @RequestHeader(value = "Toss-Signature", required = false) String signature,
            @RequestBody String body) throws IOException {

        // 시그니처 검증
        if (!webhookService.verifySignature(signature, body)) {
            log.warn("웹훅 시그니처 검증 실패");
            throw new BusinessException(ErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }

        // 페이로드 파싱
        TossWebhookPayload payload = objectMapper.readValue(body, TossWebhookPayload.class);

        // 이벤트 처리
        webhookService.processWebhook(payload);

        return ApiResponse.success();
    }
}
