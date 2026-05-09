package com.pilates.domain.member.entity;

import com.pilates.common.entity.BaseEntity;
import com.pilates.domain.instructor.entity.Instructor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 엔티티.
 * 개인정보(휴대폰, 이름, 생년월일)는 암호화 저장.
 * phone_hash(SHA-256)로 검색, phone_encrypted(AES)로 복호화 표시.
 */
@Entity
@Table(name = "members", indexes = {
        @Index(name = "idx_members_status", columnList = "status, deleted_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    /** 외부 노출용 ID (UUID, 하이픈 제거 32자) */
    @NotBlank
    @Column(name = "public_id", nullable = false, unique = true, length = 32)
    private String publicId;

    /** AES-256 암호화된 이름 */
    @NotBlank
    @Column(name = "name", nullable = false, length = 512)
    private String name;

    /** AES-256 암호화된 휴대폰 번호 (복호화하여 표시용) */
    @NotBlank
    @Column(name = "phone_encrypted", nullable = false, length = 512)
    private String phoneEncrypted;

    /** SHA-256 해시된 휴대폰 번호 (검색/중복 확인용). 탈퇴 시 NULL 처리. */
    @Column(name = "phone_hash", unique = true, length = 64)
    private String phoneHash;

    /** AES-256 암호화된 생년월일 */
    @Column(name = "birth_encrypted", length = 512)
    private String birthEncrypted;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    /** 프로필 사진 URL (Cloudflare R2). NULL이면 이니셜 fallback. */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /** 프로필 사진 업로드 시각 */
    @Column(name = "profile_image_uploaded_at")
    private java.time.LocalDateTime profileImageUploadedAt;

    /** 담당 강사 (선택). 단방향 ManyToOne, LAZY 필수. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", foreignKey = @ForeignKey(name = "fk_members_instructor"))
    private Instructor instructor;

    @Builder
    private Member(String publicId, String name, String phoneEncrypted, String phoneHash,
                   String birthEncrypted, Gender gender, MemberStatus status, Instructor instructor,
                   String profileImageUrl, java.time.LocalDateTime profileImageUploadedAt) {
        this.publicId = publicId;
        this.name = name;
        this.phoneEncrypted = phoneEncrypted;
        this.phoneHash = phoneHash;
        this.birthEncrypted = birthEncrypted;
        this.gender = gender;
        this.status = status;
        this.instructor = instructor;
        this.profileImageUrl = profileImageUrl;
        this.profileImageUploadedAt = profileImageUploadedAt;
    }

    // ── 도메인 메서드 (placeholder, 다음 단계에서 구현) ──

    /** 회원 정보 수정 */
    public void updateInfo(String name, String phoneEncrypted, String phoneHash, String birthEncrypted) {
        // TODO: 다음 단계
    }

    /** 상태 변경 */
    public void changeStatus(MemberStatus newStatus) {
        // TODO: 상태 전이 검증 로직
        this.status = newStatus;
    }

    /** 담당 강사 변경 */
    public void assignInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    /** 탈퇴 처리: soft delete + phone_hash NULL 처리 (원본은 WithdrawnMemberLog에 보관) */
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.phoneHash = null;
        this.phoneEncrypted = null;
        this.softDelete();
    }
}
