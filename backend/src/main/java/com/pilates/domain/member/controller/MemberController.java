package com.pilates.domain.member.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.member.dto.MemberResponse;
import com.pilates.domain.member.dto.MemberUpdateRequest;
import com.pilates.domain.member.service.MemberService;
import com.pilates.domain.member.service.ProfileImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 회원 API.
 * 인증 필요 (JWT Access Token).
 */
@Tag(name = "Member", description = "회원 정보 관리 (조회, 수정, 탈퇴, 프로필 사진)")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final ProfileImageService profileImageService;

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 정보를 조회한다. 암호화된 필드는 복호화하여 반환.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyInfo(@LoginMemberAnnotation LoginMember loginMember) {
        return ApiResponse.success(memberService.getMyInfo(loginMember.memberId()));
    }

    @Operation(summary = "내 정보 수정", description = "이름, 생년월일 등을 수정한다. null인 필드는 변경하지 않음. 동시성: last-write-wins.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PatchMapping("/me")
    public ApiResponse<MemberResponse> updateMyInfo(@LoginMemberAnnotation LoginMember loginMember,
                                                    @RequestBody MemberUpdateRequest request) {
        return ApiResponse.success(memberService.updateMyInfo(loginMember.memberId(), request));
    }

    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴 처리. soft delete + phone_hash NULL. 30일 후 개인정보 익명화.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@LoginMemberAnnotation LoginMember loginMember,
                                      @RequestParam(required = false) String reason) {
        memberService.withdraw(loginMember.memberId(), reason);
        return ApiResponse.success();
    }

    @Operation(summary = "프로필 사진 업로드", description = "프로필 사진을 업로드한다. 최대 5MB, JPG/PNG/WebP. 매직 넘버 검증.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파일 없음/크기 초과/형식 오류")
    })
    @PostMapping("/me/profile-image")
    public ApiResponse<Map<String, String>> uploadProfileImage(
            @LoginMemberAnnotation LoginMember loginMember,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = profileImageService.uploadProfileImage(loginMember.memberId(), file);
        return ApiResponse.success(Map.of("imageUrl", imageUrl));
    }

    @Operation(summary = "프로필 사진 삭제", description = "프로필 사진을 삭제한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/me/profile-image")
    public ApiResponse<Void> deleteProfileImage(@LoginMemberAnnotation LoginMember loginMember) {
        profileImageService.deleteProfileImage(loginMember.memberId());
        return ApiResponse.success();
    }
}
