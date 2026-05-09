package com.pilates.domain.membership.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.membership.dto.PublicMembershipPassResponse;
import com.pilates.domain.membership.service.MembershipPassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 정기권 종류 공개 조회 API.
 */
@Tag(name = "Membership Pass", description = "정기권 종류 조회")
@RestController
@RequestMapping("/api/membership-passes")
@RequiredArgsConstructor
public class PublicMembershipPassController {

    private final MembershipPassService membershipPassService;

    @Operation(summary = "정기권 종류 목록 조회", description = "활성 정기권 종류를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<PublicMembershipPassResponse>> list() {
        return ApiResponse.success(membershipPassService.listPublic());
    }

    @Operation(summary = "정기권 종류 상세 조회", description = "정기권 종류 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 종류 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<PublicMembershipPassResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(membershipPassService.getPublicDetail(id));
    }
}
