package com.pilates.domain.classroom.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.classroom.dto.ClassScheduleCreateRequest;
import com.pilates.domain.classroom.dto.ClassScheduleResponse;
import com.pilates.domain.classroom.dto.GenerateRequest;
import com.pilates.domain.classroom.scheduler.ClassScheduleGenerator;
import com.pilates.domain.classroom.service.ClassScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 수업 시간표 관리 API (관리자용).
 */
@Tag(name = "Admin - Class Schedule", description = "수업 시간표 관리 (생성, 취소, 자동 생성)")
@RestController
@RequestMapping("/api/admin/class-schedules")
@RequiredArgsConstructor
public class AdminClassScheduleController {

    private final ClassScheduleService classScheduleService;
    private final ClassScheduleGenerator classScheduleGenerator;

    @Operation(summary = "수업 단건 생성", description = "ad-hoc 수업을 수동 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강사/수업유형 없음")
    })
    @PostMapping
    public ApiResponse<ClassScheduleResponse> createSingle(
            @Valid @RequestBody ClassScheduleCreateRequest request) {
        return ApiResponse.success(classScheduleService.createSingleClass(request));
    }

    @Operation(summary = "수업 목록 조회 (날짜 범위)", description = "날짜 범위로 수업 목록을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<ClassScheduleResponse>> listByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(classScheduleService.listByDateRange(from, to));
    }

    @Operation(summary = "수업 상세 조회", description = "수업 ID로 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수업 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<ClassScheduleResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(classScheduleService.getClassDetail(id));
    }

    @Operation(summary = "수업 취소 (휴강)", description = "수업을 취소(휴강) 처리한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "취소 불가 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수업 없음")
    })
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        classScheduleService.cancelClass(id);
        return ApiResponse.success();
    }

    @Operation(summary = "수업 자동 생성", description = "고정 스케줄 기반으로 향후 N주간 수업을 자동 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 완료")
    })
    @PostMapping("/generate")
    public ApiResponse<Map<String, Integer>> generate(@Valid @RequestBody GenerateRequest request) {
        int created = classScheduleGenerator.generateUpcomingClasses(request.getWeeksOrDefault());
        return ApiResponse.success(Map.of("createdCount", created));
    }
}
