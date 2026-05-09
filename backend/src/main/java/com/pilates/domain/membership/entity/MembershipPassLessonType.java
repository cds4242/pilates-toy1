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
 * 정기권 종류-수업 유형 매핑 테이블.
 * 1개 정기권 종류가 여러 수업 유형에서 사용 가능 (예: 그룹+듀엣 겸용).
 */
@Entity
@Table(name = "membership_pass_lesson_types", indexes = {
        @Index(name = "idx_mplt_pass", columnList = "membership_pass_id"),
        @Index(name = "idx_mplt_lesson_type", columnList = "lesson_type_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_mplt_pass_lesson_type",
                columnNames = {"membership_pass_id", "lesson_type_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipPassLessonType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_pass_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mplt_membership_pass"))
    private MembershipPass membershipPass;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mplt_lesson_type"))
    private LessonType lessonType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MembershipPassLessonType(MembershipPass membershipPass, LessonType lessonType) {
        this.membershipPass = membershipPass;
        this.lessonType = lessonType;
    }
}
