package com.pilates.domain.admin.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.response.PageResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.admin.dto.AdminMemberResponse;
import com.pilates.domain.admin.dto.AdminMemberResponse.MemberDetailResponse;
import com.pilates.domain.admin.dto.AdminMemberResponse.MemoInfo;
import com.pilates.domain.admin.dto.MemoRequest;
import com.pilates.domain.admin.service.AdminMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Member", description = "관리자 회원 관리 API")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @Operation(summary = "회원 검색", description = "이름 또는 전화번호로 검색, 상태 필터")
    @GetMapping
    public ApiResponse<PageResponse<AdminMemberResponse>> searchMembers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Page<AdminMemberResponse> result = adminMemberService.searchMembers(search, status, page, size, sort);
        return ApiResponse.success(PageResponse.from(result));
    }

    @Operation(summary = "회원 상세 조회", description = "8개 도메인 통합 정보")
    @GetMapping("/{id}")
    public ApiResponse<MemberDetailResponse> getMemberDetail(@PathVariable Long id) {
        return ApiResponse.success(adminMemberService.getMemberDetail(id));
    }

    @Operation(summary = "회원 메모 목록 조회")
    @GetMapping("/{id}/memos")
    public ApiResponse<List<MemoInfo>> getMemberMemos(@PathVariable Long id) {
        MemberDetailResponse detail = adminMemberService.getMemberDetail(id);
        return ApiResponse.success(detail.memos());
    }

    @Operation(summary = "회원 메모 작성")
    @PostMapping("/{id}/memos")
    public ApiResponse<MemoInfo> createMemo(
            @PathVariable Long id,
            @Valid @RequestBody MemoRequest request,
            @LoginMemberAnnotation LoginMember loginMember) {
        return ApiResponse.success(
                adminMemberService.saveMemberMemo(id, loginMember.memberId(), request.content()));
    }

    @Operation(summary = "회원 메모 수정")
    @PatchMapping("/{id}/memos/{memoId}")
    public ApiResponse<MemoInfo> updateMemo(
            @PathVariable Long id,
            @PathVariable Long memoId,
            @Valid @RequestBody MemoRequest request,
            @LoginMemberAnnotation LoginMember loginMember) {
        return ApiResponse.success(
                adminMemberService.updateMemberMemo(memoId, loginMember.memberId(), request.content()));
    }

    @Operation(summary = "회원 메모 삭제")
    @DeleteMapping("/{id}/memos/{memoId}")
    public ApiResponse<Void> deleteMemo(
            @PathVariable Long id,
            @PathVariable Long memoId,
            @LoginMemberAnnotation LoginMember loginMember) {
        adminMemberService.deleteMemberMemo(memoId, loginMember.memberId());
        return ApiResponse.success();
    }

    @Operation(summary = "강제 탈퇴", description = "운영 정책 위반 등으로 강제 탈퇴 처리")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> forceWithdraw(@PathVariable Long id) {
        adminMemberService.forceWithdrawMember(id);
        return ApiResponse.success();
    }
}
