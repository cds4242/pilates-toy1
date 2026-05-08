package com.pilates.domain.member.entity;

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
 * 강사가 작성하는 신체 특이사항, 주의점 등.
 * BaseEntity 상속 대신 독립 — soft delete 불필요.
 */
@Entity
@Table(name = "member_memos", indexes = {
        @Index(name = "idx_member_memos_member", columnList = "member_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberMemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 메모 대상 회원. 단방향 ManyToOne. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_memos_member"))
    private Member member;

    /** 작성 강사. 단방향 ManyToOne. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_memos_instructor"))
    private Instructor instructor;

    @NotBlank
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private MemberMemo(Member member, Instructor instructor, String content) {
        this.member = member;
        this.instructor = instructor;
        this.content = content;
    }

    /** 메모 내용 수정 */
    public void updateContent(String content) {
        this.content = content;
    }
}
