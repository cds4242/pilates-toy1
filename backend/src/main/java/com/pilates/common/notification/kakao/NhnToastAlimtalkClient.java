package com.pilates.common.notification.kakao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NHN Toast 알림톡 운영 클라이언트 (스텁).
 * 실제 API 호출은 v2에서 구현 예정.
 */
@Slf4j
@Component
@Profile("prod")
public class NhnToastAlimtalkClient implements KakaoAlimtalkClient {

    // TODO [v2]: WebClient 기반 NHN Toast 알림톡 API 연동

    @Override
    public AlimtalkResponse sendAlimtalk(String phoneNumber, String templateCode, Map<String, String> templateParams) {
        log.warn("[NHN TOAST] 알림톡 발송 요청 (v2 미구현) — phone={}, template={}", phoneNumber, templateCode);
        return AlimtalkResponse.failure("NHN Toast 알림톡 미구현 (v2 예정)");
    }
}
