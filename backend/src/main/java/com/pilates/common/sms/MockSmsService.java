package com.pilates.common.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 개발용 Mock SMS 서비스.
 * 실제 발송하지 않고 콘솔에 출력한다.
 * local, local-h2, test 프로파일에서만 활성화.
 *
 * 실패 모드: phoneNumber가 "FAIL_"로 시작하면 예외.
 */
@Slf4j
@Service
@Profile({"local", "local-h2", "test", "portfolio", "demo"})
public class MockSmsService implements SmsService {

    private final AtomicInteger sendCallCount = new AtomicInteger(0);
    private volatile boolean forceFailMode = false;

    @Override
    public void send(String phoneNumber, String message) {
        sendCallCount.incrementAndGet();
        log.info("[MOCK SMS] {}: {}", phoneNumber, message);

        if (forceFailMode) {
            throw new RuntimeException("Mock SMS 강제 실패 모드");
        }
    }

    /** 호출 카운트 조회 (테스트 검증용) */
    public int getSendCallCount() {
        return sendCallCount.get();
    }

    /** 호출 카운트 초기화 (테스트 간 격리용) */
    public void resetSendCallCount() {
        sendCallCount.set(0);
    }

    /** 강제 실패 모드 설정 */
    public void setForceFailMode(boolean failMode) {
        this.forceFailMode = failMode;
    }
}
