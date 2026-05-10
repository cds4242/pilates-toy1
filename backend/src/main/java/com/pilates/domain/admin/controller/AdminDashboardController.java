package com.pilates.domain.admin.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.admin.dto.DashboardResponse;
import com.pilates.domain.admin.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Dashboard", description = "관리자 대시보드 API")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @Operation(summary = "대시보드 조회", description = "오늘 수업, 이번 주 매출, 만료 임박 정기권, 알림 데이터")
    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard() {
        return ApiResponse.success(dashboardService.getDashboard());
    }

    @Operation(summary = "매출 추이 조회", description = "주간 또는 월간 매출 추이를 offset으로 탐색")
    @GetMapping("/revenue")
    public ApiResponse<DashboardResponse.ThisWeekRevenue> getRevenue(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(defaultValue = "0") int offset) {
        return ApiResponse.success(dashboardService.getRevenue(period, offset));
    }
}
