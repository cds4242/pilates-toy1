package com.pilates.domain.attendance.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.attendance.dto.AttendanceMarkRequest;
import com.pilates.domain.attendance.dto.AttendanceResponse;
import com.pilates.domain.attendance.dto.BatchAttendanceRequest;
import com.pilates.domain.attendance.service.InstructorAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Attendance (Instructor)", description = "강사 출석 체크 API")
@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorAttendanceController {

    private final InstructorAttendanceService instructorAttendanceService;

    @Operation(summary = "단건 출석 체크", description = "예약 건에 대한 출석 상태를 마킹한다.")
    @PostMapping("/attendances/{reservationId}")
    public ApiResponse<Void> markAttendance(
            @LoginMemberAnnotation LoginMember loginMember,
            @PathVariable Long reservationId,
            @Valid @RequestBody AttendanceMarkRequest request) {
        instructorAttendanceService.markAttendance(loginMember.memberId(), reservationId, request.status());
        return ApiResponse.success();
    }

    @Operation(summary = "일괄 출석 체크", description = "수업의 전체 예약자에 대한 출석 상태를 일괄 마킹한다.")
    @PostMapping("/class-schedules/{classScheduleId}/attendances")
    public ApiResponse<Void> markBatchAttendance(
            @LoginMemberAnnotation LoginMember loginMember,
            @PathVariable Long classScheduleId,
            @Valid @RequestBody BatchAttendanceRequest request) {
        instructorAttendanceService.markBatchAttendance(
                loginMember.memberId(), classScheduleId, request.attendances());
        return ApiResponse.success();
    }

    @Operation(summary = "수업 출석 현황 조회", description = "본인 담당 수업의 예약자 출석 현황을 조회한다.")
    @GetMapping("/class-schedules/{classScheduleId}/attendances")
    public ApiResponse<List<AttendanceResponse>> listAttendanceForClass(
            @LoginMemberAnnotation LoginMember loginMember,
            @PathVariable Long classScheduleId) {
        return ApiResponse.success(
                instructorAttendanceService.listAttendanceForClass(loginMember.memberId(), classScheduleId));
    }
}
