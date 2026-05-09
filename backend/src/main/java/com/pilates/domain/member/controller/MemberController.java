package com.pilates.domain.member.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.member.dto.MemberResponse;
import com.pilates.domain.member.dto.MemberUpdateRequest;
import com.pilates.domain.member.service.MemberService;
import com.pilates.domain.member.service.ProfileImageService;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 API.
 * 인증 필요 (JWT Access Token).
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final ProfileImageService profileImageService;

    /**
     * 내 정보 조회.
     * GET /api/members/me
     */
    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyInfo(@LoginMemberAnnotation LoginMember loginMember) {
        return ApiResponse.success(memberService.getMyInfo(loginMember.memberId()));
    }

    /**
     * 내 정보 수정.
     * PATCH /api/members/me
     */
    @PatchMapping("/me")
    public ApiResponse<MemberResponse> updateMyInfo(@LoginMemberAnnotation LoginMember loginMember,
                                                    @RequestBody MemberUpdateRequest request) {
        return ApiResponse.success(memberService.updateMyInfo(loginMember.memberId(), request));
    }

    /**
     * 회원 탈퇴.
     * DELETE /api/members/me
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@LoginMemberAnnotation LoginMember loginMember,
                                      @RequestParam(required = false) String reason) {
        memberService.withdraw(loginMember.memberId(), reason);
        return ApiResponse.success();
    }

    /**
     * 프로필 사진 업로드.
     * POST /api/members/me/profile-image
     */
    @PostMapping("/me/profile-image")
    public ApiResponse<java.util.Map<String, String>> uploadProfileImage(
            @LoginMemberAnnotation LoginMember loginMember,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = profileImageService.uploadProfileImage(loginMember.memberId(), file);
        return ApiResponse.success(java.util.Map.of("imageUrl", imageUrl));
    }

    /**
     * 프로필 사진 삭제.
     * DELETE /api/members/me/profile-image
     */
    @DeleteMapping("/me/profile-image")
    public ApiResponse<Void> deleteProfileImage(@LoginMemberAnnotation LoginMember loginMember) {
        profileImageService.deleteProfileImage(loginMember.memberId());
        return ApiResponse.success();
    }
}
