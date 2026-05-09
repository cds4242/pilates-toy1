package com.pilates.domain.admin.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.admin.dto.StatisticsResponse.*;
import com.pilates.domain.admin.service.AdminStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Admin Statistics", description = "관리자 통계 API")
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService statisticsService;

    @Operation(summary = "매출 통계", description = "일별/주별/월별 매출 통계")
    @GetMapping("/revenue")
    public ApiResponse<RevenueStatistics> getRevenueStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "daily") String groupBy) {
        return ApiResponse.success(statisticsService.getRevenueStatistics(from, to, groupBy));
    }

    @Operation(summary = "회원 추이", description = "가입/탈퇴 추이, 현재 활성 회원 수")
    @GetMapping("/members")
    public ApiResponse<MemberStatistics> getMemberStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(statisticsService.getMemberStatistics(from, to));
    }

    @Operation(summary = "출석률 통계", description = "전체, 강사별, 수업유형별 출석률")
    @GetMapping("/attendance")
    public ApiResponse<AttendanceStatistics> getAttendanceStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(statisticsService.getAttendanceStatistics(from, to));
    }

    @Operation(summary = "인기 시간대", description = "시간대별, 요일별 예약 수")
    @GetMapping("/popular-times")
    public ApiResponse<PopularTimesStatistics> getPopularTimesStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(statisticsService.getPopularTimesStatistics(from, to));
    }
}
