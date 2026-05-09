package com.pilates.domain.admin.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.admin.dto.StudioSettingResponse;
import com.pilates.domain.admin.dto.StudioSettingUpdateRequest;
import com.pilates.domain.admin.service.AdminSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Settings", description = "학원 설정 API (SUPER_ADMIN 전용)")
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final AdminSettingsService settingsService;

    @Operation(summary = "학원 설정 조회")
    @GetMapping
    public ApiResponse<StudioSettingResponse> getSettings() {
        return ApiResponse.success(settingsService.getSettings());
    }

    @Operation(summary = "학원 설정 수정")
    @PatchMapping
    public ApiResponse<StudioSettingResponse> updateSettings(@RequestBody StudioSettingUpdateRequest request) {
        return ApiResponse.success(settingsService.updateSettings(request.settings()));
    }
}
