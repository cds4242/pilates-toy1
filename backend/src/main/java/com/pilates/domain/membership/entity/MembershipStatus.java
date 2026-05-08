package com.pilates.domain.membership.entity;

/** 정기권 상태 */
public enum MembershipStatus {
    ACTIVE,     // 활성
    EXPIRED,    // 만료
    EXHAUSTED,  // 소진
    HOLDING     // 홀딩(일시정지)
}
