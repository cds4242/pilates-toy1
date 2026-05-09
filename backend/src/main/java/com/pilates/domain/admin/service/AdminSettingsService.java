package com.pilates.domain.admin.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.admin.dto.StudioSettingResponse;
import com.pilates.domain.admin.dto.StudioSettingResponse.SettingItem;
import com.pilates.domain.admin.entity.StudioSetting;
import com.pilates.domain.admin.repository.StudioSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSettingsService {

    private final StudioSettingRepository studioSettingRepository;

    public StudioSettingResponse getSettings() {
        List<StudioSetting> settings = studioSettingRepository.findAll();
        List<SettingItem> items = settings.stream()
                .map(s -> new SettingItem(
                        s.getId(), s.getSettingKey(), s.getSettingValue(),
                        s.getDescription(), s.getUpdatedAt()))
                .toList();
        return new StudioSettingResponse(items);
    }

    @Transactional
    public StudioSettingResponse updateSettings(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            StudioSetting setting = studioSettingRepository.findBySettingKey(entry.getKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_SETTINGS_NOT_FOUND));
            // StudioSetting은 value 업데이트 메서드가 없으므로 직접 처리
            // 기존 엔티티에 updateValue 메서드 추가 필요
            setting.updateValue(entry.getValue());
        }
        return getSettings();
    }
}
