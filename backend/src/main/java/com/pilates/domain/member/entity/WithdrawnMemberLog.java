package com.pilates.domain.member.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 탈퇴 회원 로그.
 * 회원 탈퇴 시 phone_hash를 NULL로 변경하고, 원본 정보를 이 테이블에 보관.
 * 30일 후 익명화 스케줄러가 phone_hash_original, name_original을 삭제.
 */
@Entity
@Table(name = "withdrawn_member_logs", indexes = {
        @Index(name = "idx_withdrawn_member_logs_member", columnList = "member_id"),
        @Index(name = "idx_withdrawn_member_logs_withdrawn_at", columnList = "withdrawn_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawnMemberLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 탈퇴 전 phone_hash 원본 (30일 후 익명화 시 NULL 처리) */
    @Column(name = "phone_hash_original", length = 64)
    private String phoneHashOriginal;

    /** 탈퇴 전 암호화된 이름 원본 (30일 후 익명화 시 NULL 처리) */
    @Column(name = "name_original", length = 50)
    private String nameOriginal;

    /** 탈퇴 전 암호화된 생년월일 원본 (30일 후 익명화 시 NULL 처리) */
    @Column(name = "birth_encrypted_original", length = 255)
    private String birthEncryptedOriginal;

    @NotNull
    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;

    @Column(name = "withdrawal_reason", length = 255)
    private String withdrawalReason;

    /** 익명화 처리 여부 */
    @Column(name = "anonymized", nullable = false)
    private boolean anonymized;

    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;

    @Builder
    private WithdrawnMemberLog(Long memberId, String phoneHashOriginal, String nameOriginal,
                               String birthEncryptedOriginal,
                               LocalDateTime withdrawnAt, String withdrawalReason) {
        this.memberId = memberId;
        this.phoneHashOriginal = phoneHashOriginal;
        this.nameOriginal = nameOriginal;
        this.birthEncryptedOriginal = birthEncryptedOriginal;
        this.withdrawnAt = withdrawnAt;
        this.withdrawalReason = withdrawalReason;
        this.anonymized = false;
    }

    /** 30일 후 익명화 처리 */
    public void anonymize() {
        this.phoneHashOriginal = null;
        this.nameOriginal = null;
        this.birthEncryptedOriginal = null;
        this.anonymized = true;
        this.anonymizedAt = LocalDateTime.now();
    }
}
