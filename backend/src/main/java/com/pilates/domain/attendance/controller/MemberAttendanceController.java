package com.pilates.domain.attendance.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.response.PageResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.attendance.dto.AttendanceRateResponse;
import com.pilates.domain.attendance.dto.AttendanceResponse;
import com.pilates.domain.attendance.service.MemberAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Attendance (Member)", description = "회원 본인 출석 이력 API")
@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MemberAttendanceController {

    private final MemberAttendanceService memberAttendanceService;

    @Operation(summary = "내 출석 이력 조회", description = "본인 출석 이력을 페이지네이션으로 조회한다.")
    @GetMapping("/attendances")
    public ApiResponse<PageResponse<AttendanceResponse>> listMyAttendance(
            @LoginMemberAnnotation LoginMember loginMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                PageResponse.from(memberAttendanceService.listMyAttendance(loginMember.memberId(), page, size)));
    }

    @Operation(summary = "내 출석률 조회", description = "기간별 출석률을 조회한다. period: 30d, 90d, all")
    @GetMapping("/attendance-rate")
    public ApiResponse<AttendanceRateResponse> getMyAttendanceRate(
            @LoginMemberAnnotation LoginMember loginMember,
            @RequestParam(defaultValue = "all") String period) {
        return ApiResponse.success(
                memberAttendanceService.getMyAttendanceRate(loginMember.memberId(), period));
    }
}
