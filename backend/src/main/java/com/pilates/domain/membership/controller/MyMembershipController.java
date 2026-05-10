package com.pilates.domain.membership.controller;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.membership.dto.MembershipIssueRequest;
import com.pilates.domain.membership.dto.MembershipResponse;
import com.pilates.domain.membership.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 내 정기권 API (회원용).
 * 인증 필요 (JWT Access Token).
 */
@Tag(name = "My Membership", description = "내 정기권 조회")
@RestController
@RequestMapping("/api/members/me/memberships")
@RequiredArgsConstructor
public class MyMembershipController {

    private final MembershipService membershipService;

    @Operation(summary = "내 정기권 목록", description = "로그인한 회원의 정기권 목록을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public ApiResponse<List<MembershipResponse>> myMemberships(
            @LoginMemberAnnotation LoginMember loginMember) {
        return ApiResponse.success(membershipService.listByMember(loginMember.memberId()));
    }

    @Operation(summary = "내 정기권 상세", description = "로그인한 회원의 정기권 상세를 조회한다. 본인 소유가 아니면 404.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<MembershipResponse> myMembershipDetail(
            @LoginMemberAnnotation LoginMember loginMember,
            @PathVariable Long id) {
        MembershipResponse response = membershipService.getDetail(id);

        // 본인 소유 검증
        if (!response.memberId().equals(loginMember.memberId())) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND);
        }

        return ApiResponse.success(response);
    }

    @Operation(summary = "수강권 구매 (시연용)", description = "회원이 직접 수강권을 구매한다. 로컬 시연용으로 결제 없이 즉시 발급.")
    @PostMapping("/purchase")
    public ApiResponse<MembershipResponse> purchase(
            @LoginMemberAnnotation LoginMember loginMember,
            @Valid @RequestBody MembershipPurchaseRequest request) {
        MembershipIssueRequest issueRequest = new MembershipIssueRequest(
                loginMember.memberId(),
                request.totalCount(),
                request.price(),
                request.validityDays(),
                request.unlimited(),
                request.lessonTypeIds(),
                request.membershipPassId()
        );
        return ApiResponse.success(membershipService.issueMembership(issueRequest));
    }

    public record MembershipPurchaseRequest(
            Integer totalCount,
            java.math.BigDecimal price,
            Integer validityDays,
            boolean unlimited,
            List<Long> lessonTypeIds,
            Long membershipPassId
    ) {}
}
