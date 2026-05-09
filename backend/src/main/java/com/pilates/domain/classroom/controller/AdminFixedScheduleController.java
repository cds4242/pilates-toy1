package com.pilates.domain.classroom.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.classroom.dto.FixedScheduleRequest;
import com.pilates.domain.classroom.dto.FixedScheduleResponse;
import com.pilates.domain.classroom.service.FixedScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 고정 스케줄 관리 API (관리자용).
 */
@Tag(name = "Admin - Fixed Schedule", description = "고정 반복 스케줄 관리")
@RestController
@RequestMapping("/api/admin/fixed-schedules")
@RequiredArgsConstructor
public class AdminFixedScheduleController {

    private final FixedScheduleService fixedScheduleService;

    @Operation(summary = "고정 스케줄 생성", description = "주간 반복 고정 스케줄을 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "근무 시간 외"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "시간 충돌")
    })
    @PostMapping
    public ApiResponse<FixedScheduleResponse> create(@Valid @RequestBody FixedScheduleRequest request) {
        return ApiResponse.success(fixedScheduleService.createFixedSchedule(request));
    }

    @Operation(summary = "고정 스케줄 목록", description = "고정 스케줄 목록을 조회한다. instructorId 필터 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<FixedScheduleResponse>> list(
            @RequestParam(required = false) Long instructorId) {
        if (instructorId != null) {
            return ApiResponse.success(fixedScheduleService.listByInstructor(instructorId));
        }
        return ApiResponse.success(fixedScheduleService.listAll());
    }

    @Operation(summary = "고정 스케줄 수정", description = "고정 스케줄 정보를 수정한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "스케줄 없음")
    })
    @PatchMapping("/{id}")
    public ApiResponse<FixedScheduleResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody FixedScheduleRequest request) {
        return ApiResponse.success(fixedScheduleService.updateFixedSchedule(id, request));
    }

    @Operation(summary = "고정 스케줄 비활성화", description = "고정 스케줄을 비활성화한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비활성화 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "스케줄 없음")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        fixedScheduleService.deactivateFixedSchedule(id);
        return ApiResponse.success();
    }
}
