package com.pilates.common.notification.kakao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 개발/테스트용 Mock 알림톡 클라이언트.
 * 실제 발송하지 않고 콘솔에 출력한다.
 *
 * 실패 모드: templateCode가 "FAIL_"로 시작하면 의도적 실패.
 */
@Slf4j
@Component
@Profile({"local", "local-h2", "test", "portfolio"})
public class MockKakaoAlimtalkClient implements KakaoAlimtalkClient {

    private final AtomicInteger sendCallCount = new AtomicInteger(0);

    @Override
    public AlimtalkResponse sendAlimtalk(String phoneNumber, String templateCode, Map<String, String> templateParams) {
        sendCallCount.incrementAndGet();
        log.info("[MOCK ALIMTALK] {}: {} {}", phoneNumber, templateCode, templateParams);

        if (templateCode != null && templateCode.startsWith("FAIL_")) {
            log.info("[MOCK ALIMTALK] 발송 실패 (의도적) — templateCode={}", templateCode);
            return AlimtalkResponse.failure("Mock 알림톡 발송 실패: " + templateCode);
        }

        String messageId = "mock-alimtalk-" + UUID.randomUUID().toString().substring(0, 8);
        return AlimtalkResponse.success(messageId);
    }

    /** 호출 카운트 조회 (테스트 검증용) */
    public int getSendCallCount() {
        return sendCallCount.get();
    }

    /** 호출 카운트 초기화 (테스트 간 격리용) */
    public void resetSendCallCount() {
        sendCallCount.set(0);
    }
}
