package com.pilates.domain.classroom.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.classroom.dto.ClassScheduleResponse;
import com.pilates.domain.classroom.service.ClassScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 수업 시간표 조회 API (회원용).
 * 예약 화면에서 사용하는 수업 목록/상세 조회.
 */
@Tag(name = "Class Schedule", description = "수업 시간표 조회 (회원 예약용)")
@RestController
@RequestMapping("/api/class-schedules")
@RequiredArgsConstructor
public class MemberClassScheduleController {

    private final ClassScheduleService classScheduleService;

    // TODO [STEP 8 reservation]: 회원 시간표 조회 시 본인 예약 상태 표시
    // 1. ClassScheduleResponse에 myReservationStatus 필드 추가
    //    - NOT_RESERVED / RESERVED / CANCELLED / FULL (정원 마감)
    // 2. ClassSchedule 조회 시 회원 ID로 reservations 조인
    // 3. 의뢰인 시안 A의 "예약 가능/마감" UX 반영
    @Operation(summary = "수업 목록 조회", description = "날짜 범위로 예약 가능한 수업 목록을 조회한다 (취소 제외).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<ClassScheduleResponse>> listByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(classScheduleService.listByDateRange(from, to));
    }

    // TODO [STEP 8 reservation]: 수업 상세에서 본인 예약 여부 + 예약자 수 표시
    @Operation(summary = "수업 상세 조회", description = "수업 ID로 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수업 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<ClassScheduleResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(classScheduleService.getClassDetail(id));
    }
}
