package com.pilates.domain.attendance.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.attendance.dto.AttendanceRateResponse;
import com.pilates.domain.attendance.dto.NoShowCountResponse;
import com.pilates.domain.attendance.service.MemberAttendanceService;
import com.pilates.domain.attendance.repository.AttendanceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Attendance (Admin)", description = "관리자 출석 통계 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAttendanceController {

    private final MemberAttendanceService memberAttendanceService;
    private final AttendanceRepository attendanceRepository;

    @Operation(summary = "회원 출석률 조회", description = "특정 회원의 출석률을 조회한다.")
    @GetMapping("/members/{memberId}/attendance-rate")
    public ApiResponse<AttendanceRateResponse> getMemberAttendanceRate(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "all") String period) {
        return ApiResponse.success(memberAttendanceService.getMyAttendanceRate(memberId, period));
    }

    @Operation(summary = "회원별 노쇼 횟수", description = "기간 내 회원별 노쇼 횟수를 조회한다.")
    @GetMapping("/attendances/no-show-counts")
    public ApiResponse<List<NoShowCountResponse>> getNoShowCounts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<Object[]> results = attendanceRepository.countNoShowByMemberAndPeriod(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        List<NoShowCountResponse> response = results.stream()
                .map(row -> new NoShowCountResponse((Long) row[0], (Long) row[1]))
                .toList();
        return ApiResponse.success(response);
    }
}
