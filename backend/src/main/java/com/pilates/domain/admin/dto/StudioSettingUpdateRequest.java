package com.pilates.domain.admin.dto;

import java.util.Map;

public record StudioSettingUpdateRequest(
        Map<String, String> settings
) {}
