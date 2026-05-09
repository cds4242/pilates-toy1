package com.pilates.domain.classroom.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.classroom.dto.LessonTypeRequest;
import com.pilates.domain.classroom.dto.LessonTypeResponse;
import com.pilates.domain.classroom.service.LessonTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 수업 유형 관리 API (관리자용).
 */
@Tag(name = "Admin - Lesson Type", description = "수업 유형 관리 (생성, 수정, 비활성화)")
@RestController
@RequestMapping("/api/admin/lesson-types")
@RequiredArgsConstructor
public class AdminLessonTypeController {

    private final LessonTypeService lessonTypeService;

    @Operation(summary = "수업 유형 생성", description = "새 수업 유형을 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이름 중복")
    })
    @PostMapping
    public ApiResponse<LessonTypeResponse> create(@Valid @RequestBody LessonTypeRequest request) {
        return ApiResponse.success(lessonTypeService.createLessonType(request));
    }

    @Operation(summary = "전체 수업 유형 목록", description = "전체 수업 유형 목록을 조회한다 (비활성 포함).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<LessonTypeResponse>> listAll() {
        return ApiResponse.success(lessonTypeService.listAllLessonTypes());
    }

    @Operation(summary = "수업 유형 수정", description = "수업 유형 정보를 수정한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수업 유형 없음")
    })
    @PatchMapping("/{id}")
    public ApiResponse<LessonTypeResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody LessonTypeRequest request) {
        return ApiResponse.success(lessonTypeService.updateLessonType(id, request));
    }

    @Operation(summary = "수업 유형 비활성화", description = "수업 유형을 비활성화한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비활성화 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수업 유형 없음")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        lessonTypeService.deactivateLessonType(id);
        return ApiResponse.success();
    }
}
