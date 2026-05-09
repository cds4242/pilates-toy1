package com.pilates.domain.payment.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.payment.dto.TossWebhookPayload;
import com.pilates.domain.payment.entity.Payment;
import com.pilates.domain.payment.entity.PaymentStatus;
import com.pilates.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * 토스 웹훅 처리 서비스.
 * 시그니처 검증 (HMAC-SHA256) + 멱등성 (Redis) + 상태 보정.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TossWebhookService {

    private static final String WEBHOOK_EVENT_KEY = "webhook:event:";
    private static final Duration EVENT_TTL = Duration.ofHours(24);

    private final PaymentRepository paymentRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.toss.secret-key:test_sk_DUMMY}")
    private String tossSecretKey;

    /**
     * 웹훅 시그니처 검증 (HMAC-SHA256).
     * @param signature 요청 헤더의 시그니처
     * @param body 요청 본문 (raw)
     * @return 유효하면 true
     */
    public boolean verifySignature(String signature, String body) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tossSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getEncoder().encodeToString(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("웹훅 시그니처 검증 실패", e);
            return false;
        }
    }

    /**
     * 웹훅 이벤트 처리.
     * 멱등성: 같은 paymentKey+eventType 조합은 24시간 내 중복 처리 방지.
     * 상태 보정: DB 상태와 토스 상태 불일치 시 보정.
     */
    @Transactional
    public void processWebhook(TossWebhookPayload payload) {
        String eventKey = payload.data().paymentKey() + ":" + payload.eventType();

        // 멱등성 검사
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(
                WEBHOOK_EVENT_KEY + eventKey, "1", EVENT_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("중복 웹훅 이벤트 무시: {}", eventKey);
            return;
        }

        // 결제 조회
        Payment payment = paymentRepository.findByOrderId(payload.data().orderId()).orElse(null);
        if (payment == null) {
            log.warn("웹훅: 알 수 없는 orderId={}", payload.data().orderId());
            return;
        }

        // 상태 보정
        String tossStatus = payload.data().status();
        log.info("웹훅 수신: orderId={}, eventType={}, tossStatus={}, dbStatus={}",
                payload.data().orderId(), payload.eventType(), tossStatus, payment.getStatus());

        if ("DONE".equals(tossStatus) && payment.getStatus() == PaymentStatus.PENDING) {
            // 토스에서 승인됐는데 DB가 아직 PENDING → confirm 누락 보정
            log.warn("웹훅 상태 보정: PENDING → COMPLETED (orderId={})", payload.data().orderId());
            payment.confirm(payload.data().paymentKey(), null, java.time.LocalDateTime.now());
            // TODO: 정기권 발급도 보정 필요 (복잡 → v2에서 상세 처리)
        } else if ("CANCELED".equals(tossStatus) && payment.getStatus() == PaymentStatus.COMPLETED) {
            // 토스에서 취소됐는데 DB가 COMPLETED → 환불 보정
            log.warn("웹훅 상태 보정: COMPLETED → REFUNDED (orderId={})", payload.data().orderId());
            payment.refund(payment.getAmount(), "토스 웹훅 보정");
        }
    }
}
