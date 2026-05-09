package com.pilates.domain.membership.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.membership.dto.*;
import com.pilates.domain.membership.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 정기권 관리 API (관리자용).
 */
@Tag(name = "Membership (Admin)", description = "정기권 관리 (발급, 조회, 일시정지)")
@RestController
@RequestMapping("/api/admin/memberships")
@RequiredArgsConstructor
public class AdminMembershipController {

    private final MembershipService membershipService;

    @Operation(summary = "정기권 발급", description = "회원에게 정기권을 발급한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원/수업유형 없음")
    })
    @PostMapping
    public ApiResponse<MembershipResponse> issue(@Valid @RequestBody MembershipIssueRequest request) {
        return ApiResponse.success(membershipService.issueMembership(request));
    }

    @Operation(summary = "정기권 목록 조회", description = "회원 ID로 정기권 목록을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<MembershipResponse>> listByMember(@RequestParam Long memberId) {
        return ApiResponse.success(membershipService.listByMember(memberId));
    }

    @Operation(summary = "정기권 상세 조회", description = "정기권 ID로 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<MembershipResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(membershipService.getDetail(id));
    }

    @Operation(summary = "정기권 일시정지", description = "정기권을 일시정지 처리한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일시정지 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "일시정지 불가 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 없음")
    })
    @PostMapping("/{id}/hold")
    public ApiResponse<MembershipHoldingResponse> hold(@PathVariable Long id,
                                                        @Valid @RequestBody MembershipHoldRequest request) {
        return ApiResponse.success(membershipService.holdMembership(id, request));
    }

    @Operation(summary = "정기권 일시정지 해제", description = "정기권 일시정지를 해제하고 유효기간을 연장한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "일시정지 상태 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 없음")
    })
    @PostMapping("/{id}/release-hold")
    public ApiResponse<Void> releaseHold(@PathVariable Long id) {
        membershipService.releaseHold(id);
        return ApiResponse.success();
    }

    @Operation(summary = "정기권 홀딩 이력 조회", description = "정기권의 일시정지 이력을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 없음")
    })
    @GetMapping("/{id}/holdings")
    public ApiResponse<List<MembershipHoldingResponse>> getHoldings(@PathVariable Long id) {
        return ApiResponse.success(membershipService.getHoldings(id));
    }
}
