package com.pilates.common.notification.kakao;

import java.util.Map;

/**
 * 카카오 알림톡 클라이언트 인터페이스.
 * 실제 구현체는 운영 프로파일에서, Mock은 로컬/테스트에서 사용한다.
 */
public interface KakaoAlimtalkClient {

    /**
     * 알림톡 발송.
     *
     * @param phoneNumber   수신 번호 (정규화된 11자리)
     * @param templateCode  템플릿 코드
     * @param templateParams 템플릿 변수 (key-value)
     * @return 발송 결과
     */
    AlimtalkResponse sendAlimtalk(String phoneNumber, String templateCode, Map<String, String> templateParams);
}
