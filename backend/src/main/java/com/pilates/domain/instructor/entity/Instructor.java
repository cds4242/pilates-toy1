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

    @Column(name = "phone", length = 20)
    private String phone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InstructorStatus status;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Builder
    private Instructor(String publicId, String name, String phone,
                       InstructorStatus status, String profileImageUrl) {
        this.publicId = publicId;
        this.name = name;
        this.phone = phone;
        this.status = status;
        this.profileImageUrl = profileImageUrl;
    }

    /** 강사 정보 수정. null이 아닌 필드만 업데이트. */
    public void updateInfo(String name, String phone, String profileImageUrl) {
        if (name != null && !name.isBlank()) this.name = name;
        if (phone != null) this.phone = phone;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
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
}
