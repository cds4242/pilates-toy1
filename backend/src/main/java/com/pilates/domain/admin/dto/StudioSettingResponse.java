package com.pilates.domain.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StudioSettingResponse(
        List<SettingItem> settings
) {
    public record SettingItem(
            Long id,
            String key,
            String value,
            String description,
            LocalDateTime updatedAt
    ) {}
}
