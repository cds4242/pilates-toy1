package com.pilates.domain.instructor.entity;

import com.pilates.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 강사 엔티티.
 * phone은 AES-256 암호화(phone_encrypted) + SHA-256 해시(phone_hash) 이중 저장.
 */
@Entity
@Table(name = "instructors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instructor extends BaseEntity {

    @NotBlank
    @Column(name = "public_id", nullable = false, unique = true, length = 32)
    private String publicId;

    @NotBlank
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** AES-256 암호화된 휴대폰 번호 (복호화하여 표시/발송용) */
    @Column(name = "phone_encrypted", length = 500)
    private String phoneEncrypted;

    /** SHA-256 해시된 휴대폰 번호 (검색/중복 확인용) */
    @Column(name = "phone_hash", length = 64)
    private String phoneHash;

    /** @deprecated V10 마이그레이션 후 사용 안 함. phone_encrypted 사용. */
    @Deprecated
    @Column(name = "phone", length = 20)
    private String phone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InstructorStatus status;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Column(name = "specialty", length = 200)
    private String specialty;

    @Column(name = "certification", length = 500)
    private String certification;

    @Column(name = "working_days", length = 100)
    private String workingDays;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Builder
    private Instructor(String publicId, String name, String phoneEncrypted, String phoneHash,
                       InstructorStatus status, String profileImageUrl) {
        this.publicId = publicId;
        this.name = name;
        this.phoneEncrypted = phoneEncrypted;
        this.phoneHash = phoneHash;
        this.status = status;
        this.profileImageUrl = profileImageUrl;
    }

    /** 강사 정보 수정. null이 아닌 필드만 업데이트. */
    public void updateInfo(String name, String phoneEncrypted, String phoneHash, String profileImageUrl) {
        if (name != null && !name.isBlank()) this.name = name;
        if (phoneEncrypted != null) {
            this.phoneEncrypted = phoneEncrypted;
            this.phoneHash = phoneHash;
        }
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    /** 프로필 확장 정보 수정. */
    public void updateProfile(String email, String address, java.time.LocalDate birthDate,
                              String specialty, String certification, String workingDays, String memo) {
        if (email != null) this.email = email;
        if (address != null) this.address = address;
        if (birthDate != null) this.birthDate = birthDate;
        if (specialty != null) this.specialty = specialty;
        if (certification != null) this.certification = certification;
        if (workingDays != null) this.workingDays = workingDays;
        if (memo != null) this.memo = memo;
    }

    /** 비활성화 */
    public void deactivate() {
        this.status = InstructorStatus.INACTIVE;
    }

    /** 활성화 */
    public void activate() {
        this.status = InstructorStatus.ACTIVE;
    }

    /** 활성 상태인지 */
    public boolean isActive() {
        return this.status == InstructorStatus.ACTIVE;
    }

    /** phone 평문 → 암호화 마이그레이션 */
    public void migratePhone(String phoneEncrypted, String phoneHash) {
        this.phoneEncrypted = phoneEncrypted;
        this.phoneHash = phoneHash;
    }
}
