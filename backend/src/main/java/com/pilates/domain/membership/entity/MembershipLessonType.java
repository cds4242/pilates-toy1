package com.pilates.domain.membership.entity;

import com.pilates.domain.classroom.entity.LessonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 정기권-수업 유형 매핑 테이블.
 * 1개 정기권이 여러 수업 유형에서 사용 가능 (예: 개인+듀엣 겸용 정기권).
 * 단순 1:1 케이스도 이 구조로 처리.
 */
@Entity
@Table(name = "membership_lesson_types", indexes = {
        @Index(name = "idx_mlt_membership", columnList = "membership_id"),
        @Index(name = "idx_mlt_lesson_type", columnList = "lesson_type_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_mlt_membership_lesson_type",
                columnNames = {"membership_id", "lesson_type_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipLessonType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mlt_membership"))
    private Membership membership;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mlt_lesson_type"))
    private LessonType lessonType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MembershipLessonType(Membership membership, LessonType lessonType) {
        this.membership = membership;
        this.lessonType = lessonType;
    }
}
