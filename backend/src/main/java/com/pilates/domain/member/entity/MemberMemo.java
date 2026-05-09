package com.pilates.domain.member.entity;

import com.pilates.domain.admin.entity.Admin;
import com.pilates.domain.instructor.entity.Instructor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 회원 메모.
 * 강사 또는 관리자가 작성하는 신체 특이사항, 주의점 등.
 * instructor_id와 admin_id 중 하나는 반드시 존재.
 */
@Entity
@Table(name = "member_memos", indexes = {
        @Index(name = "idx_member_memos_member", columnList = "member_id"),
        @Index(name = "idx_member_memos_member_deleted", columnList = "member_id, deleted_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberMemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_memos_member"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", foreignKey = @ForeignKey(name = "fk_member_memos_instructor"))
    private Instructor instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", foreignKey = @ForeignKey(name = "fk_member_memos_admin"))
    private Admin admin;

    @NotBlank
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private MemberMemo(Member member, Instructor instructor, Admin admin, String content) {
        this.member = member;
        this.instructor = instructor;
        this.admin = admin;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /** 작성자(admin) ID 반환. 강사 메모면 null. */
    public Long getWriterAdminId() {
        return this.admin != null ? this.admin.getId() : null;
    }
}
