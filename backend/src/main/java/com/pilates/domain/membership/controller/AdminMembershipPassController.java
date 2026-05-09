package com.pilates.domain.membership.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.membership.dto.*;
import com.pilates.domain.membership.service.MembershipPassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 정기권 종류 관리 API (관리자용).
 */
@Tag(name = "Membership Pass (Admin)", description = "정기권 종류 관리")
@RestController
@RequestMapping("/api/admin/membership-passes")
@RequiredArgsConstructor
public class AdminMembershipPassController {

    private final MembershipPassService membershipPassService;

    @Operation(summary = "정기권 종류 생성", description = "새로운 정기권 종류(상품)를 등록한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이름 중복")
    })
    @PostMapping
    public ApiResponse<MembershipPassResponse> create(@Valid @RequestBody MembershipPassCreateRequest request) {
        return ApiResponse.success(membershipPassService.createMembershipPass(request));
    }

    @Operation(summary = "정기권 종류 목록 조회", description = "모든 활성 정기권 종류를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<MembershipPassResponse>> listAll() {
        return ApiResponse.success(membershipPassService.listActive());
    }

    @Operation(summary = "정기권 종류 상세 조회", description = "정기권 종류 ID로 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 종류 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<MembershipPassResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(membershipPassService.getDetail(id));
    }

    @Operation(summary = "정기권 종류 수정", description = "정기권 종류 정보를 부분 수정한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 종류 없음")
    })
    @PatchMapping("/{id}")
    public ApiResponse<MembershipPassResponse> update(@PathVariable Long id,
                                                       @RequestBody MembershipPassUpdateRequest request) {
        return ApiResponse.success(membershipPassService.updateMembershipPass(id, request));
    }

    @Operation(summary = "수업 유형 매핑 변경", description = "정기권 종류의 수업 유형 매핑을 변경한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 종류 없음")
    })
    @PatchMapping("/{id}/lesson-types")
    public ApiResponse<Void> updateLessonTypes(@PathVariable Long id,
                                                @Valid @RequestBody LessonTypeMappingRequest request) {
        membershipPassService.updateLessonTypeMappings(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "정기권 종류 비활성화", description = "정기권 종류를 비활성화(논리 삭제)한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비활성화 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 종류 없음")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        membershipPassService.deactivate(id);
        return ApiResponse.success();
    }
}
