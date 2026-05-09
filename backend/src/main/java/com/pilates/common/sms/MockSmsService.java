package com.pilates.common.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 개발용 Mock SMS 서비스.
 * 실제 발송하지 않고 콘솔에 출력한다.
 * local, local-h2, test 프로파일에서만 활성화.
 */
@Slf4j
@Service
@Profile({"local", "local-h2", "test"})
public class MockSmsService implements SmsService {

    @Override
    public void send(String phoneNumber, String message) {
        log.info("[MOCK SMS] {}: {}", phoneNumber, message);
    }
}
