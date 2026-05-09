package com.pilates.domain.classroom.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.classroom.dto.LessonTypeResponse;
import com.pilates.domain.classroom.service.LessonTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 수업 유형 공개 API.
 */
@Tag(name = "Lesson Type", description = "수업 유형 공개 조회")
@RestController
@RequestMapping("/api/lesson-types")
@RequiredArgsConstructor
public class PublicLessonTypeController {

    private final LessonTypeService lessonTypeService;

    @Operation(summary = "활성 수업 유형 목록", description = "활성 상태인 수업 유형 목록을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<LessonTypeResponse>> listActive() {
        return ApiResponse.success(lessonTypeService.listActiveLessonTypes());
    }
}
