package com.pilates.domain.member.dto;

/**
 * 회원 정보 조회 응답 DTO.
 */
public record MemberResponse(
        String publicId,
        String name,
        String phoneNumber,
        String gender,
        String birthDate,
        String status,
        String profileImageUrl,
        String createdAt
) {
}
