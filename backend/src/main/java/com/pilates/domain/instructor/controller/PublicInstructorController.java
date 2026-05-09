package com.pilates.domain.instructor.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.instructor.dto.PublicInstructorResponse;
import com.pilates.domain.instructor.service.InstructorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 강사 공개 API (회원/비회원용).
 */
@Tag(name = "Instructor", description = "강사 공개 조회")
@RestController
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
public class PublicInstructorController {

    private final InstructorService instructorService;

    @Operation(summary = "활성 강사 목록", description = "활성 상태인 강사 목록을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<PublicInstructorResponse>> listActive() {
        return ApiResponse.success(instructorService.listActiveInstructors());
    }

    @Operation(summary = "강사 상세 조회", description = "공개 ID로 강사 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "강사 없음")
    })
    @GetMapping("/{publicId}")
    public ApiResponse<PublicInstructorResponse> getByPublicId(@PathVariable String publicId) {
        return ApiResponse.success(instructorService.getInstructorByPublicId(publicId));
    }
}
