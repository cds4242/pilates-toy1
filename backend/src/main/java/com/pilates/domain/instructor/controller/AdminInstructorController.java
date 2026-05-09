package com.pilates.domain.instructor.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.instructor.dto.*;
import com.pilates.domain.instructor.service.InstructorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 강사 관리 API (관리자용).
 */
@Tag(name = "Admin - Instructor", description = "강사 관리 (등록, 수정, 비활성화, 근무 시간 설정)")
@RestController
@RequestMapping("/api/admin/instructors")
@RequiredArgsConstructor
public class AdminInstructorController {

    private final InstructorService instructorService;

    @Operation(summary = "강사 등록", description = "새 강사를 등록한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류")
    })
    @PostMapping
    public ApiResponse<InstructorResponse> register(@Valid @RequestBody InstructorRegisterRequest request) {
        return ApiResponse.success(instructorService.registerInstructor(request));
    }

    @Operation(summary = "전체 강사 목록", description = "삭제되지 않은 전체 강사 목록을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<InstructorResponse>> listAll() {
        return ApiResponse.success(instructorService.listAllInstructors());
    }

    @Operation(summary = "강사 상세 조회", description = "ID로 강사 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강사 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<InstructorResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(instructorService.getInstructor(id));
    }

    @Operation(summary = "강사 정보 수정", description = "강사 정보를 수정한다. null 필드는 변경하지 않음.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강사 없음")
    })
    @PatchMapping("/{id}")
    public ApiResponse<InstructorResponse> update(@PathVariable Long id,
                                                   @RequestBody InstructorUpdateRequest request) {
        return ApiResponse.success(instructorService.updateInstructor(id, request));
    }

    @Operation(summary = "강사 비활성화", description = "강사를 비활성화(soft delete)한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비활성화 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 비활성 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강사 없음")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        instructorService.deactivateInstructor(id);
        return ApiResponse.success();
    }

    @Operation(summary = "강사 활성화", description = "비활성화된 강사를 다시 활성화한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활성화 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 활성 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강사 없음")
    })
    @PostMapping("/{id}/activate")
    public ApiResponse<InstructorResponse> activate(@PathVariable Long id) {
        return ApiResponse.success(instructorService.activateInstructor(id));
    }

    @Operation(summary = "강사 근무 가능 시간 설정", description = "강사의 근무 가능 시간을 전체 교체한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "시간 겹침")
    })
    @PutMapping("/{id}/available-times")
    public ApiResponse<List<AvailableTimeResponse>> setAvailableTimes(
            @PathVariable Long id,
            @Valid @RequestBody List<AvailableTimeRequest> requests) {
        return ApiResponse.success(instructorService.setAvailableTimes(id, requests));
    }

    @Operation(summary = "강사 근무 가능 시간 조회", description = "강사의 근무 가능 시간 목록을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/{id}/available-times")
    public ApiResponse<List<AvailableTimeResponse>> getAvailableTimes(@PathVariable Long id) {
        return ApiResponse.success(instructorService.getAvailableTimes(id));
    }
}
