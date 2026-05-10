package com.pilates.domain.admin.entity;

import com.pilates.common.entity.BaseEntity;
import com.pilates.domain.instructor.entity.Instructor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 계정 엔티티.
 * 강사 역할의 관리자는 instructor_id로 강사 테이블과 연결.
 * password_hash: BCrypt 해시 저장.
 */
@Entity
@Table(name = "admins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin extends BaseEntity {

    @NotBlank
    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    /** BCrypt 해시된 비밀번호 */
    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    @NotBlank
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private AdminRole role;

    /** 강사 연결 (강사 역할인 경우에만). 단방향 ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id",
            foreignKey = @ForeignKey(name = "fk_admins_instructor"))
    private Instructor instructor;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    private Admin(String loginId, String passwordHash, String name,
                  AdminRole role, Instructor instructor, boolean active) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.instructor = instructor;
        this.active = active;
    }

    /** 마지막 로그인 시간 갱신 */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /** 강사 연결 (시드용). */
    public void linkInstructor(Instructor instructor) {
        this.instructor = instructor;
    }
}
