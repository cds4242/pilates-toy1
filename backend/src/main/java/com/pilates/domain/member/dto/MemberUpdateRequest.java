package com.pilates.domain.member.dto;

/**
 * 회원 정보 수정 요청 DTO.
 * null인 필드는 변경하지 않는다.
 */
public record MemberUpdateRequest(
        String name,
        String birthDate
) {
}
