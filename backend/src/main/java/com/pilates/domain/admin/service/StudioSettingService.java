package com.pilates.domain.admin.service;

import com.pilates.domain.admin.repository.StudioSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * studio_settings 테이블에서 운영 설정값을 읽는 서비스.
 * 모든 도메인 서비스에서 하드코딩 대신 이 서비스를 통해 설정값을 조회한다.
 */
@Service
@RequiredArgsConstructor
public class StudioSettingService {

    private final StudioSettingRepository studioSettingRepository;

    /** 무료 취소 가능 시간 (시간). 기본값 2. */
    public int getCancelDeadlineHours() {
        return getInt("CANCEL_DEADLINE_HOURS", 2);
    }

    /** 무제한권 월 최대 이용 횟수. 기본값 30. */
    public int getUnlimitedMonthlyLimit() {
        return getInt("UNLIMITED_MONTHLY_LIMIT", 30);
    }

    /** 노쇼 자동 처리 시간 (분). 기본값 30. */
    public int getNoShowAutoMarkMinutes() {
        return getInt("NO_SHOW_AUTO_MARK_MINUTES", 30);
    }

    private int getInt(String key, int defaultValue) {
        return studioSettingRepository.findBySettingKey(key)
                .map(s -> {
                    try { return Integer.parseInt(s.getSettingValue()); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }
}
