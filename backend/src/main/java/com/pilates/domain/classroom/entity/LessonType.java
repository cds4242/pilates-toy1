package com.pilates.domain.classroom.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
 * 수업 유형 마스터.
 * 관리자가 동적으로 추가/수정 가능 (하드코딩 아님).
 * 예: 개인(정원1), 듀엣(정원2), 그룹(정원8), 체험(정원1)
 */
@Entity
@Table(name = "lesson_types")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 정원 */
    @NotNull
    @Min(1)
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    /** 수업 시간 (분) */
    @NotNull
    @Min(1)
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /** 예약 시 정기권에서 차감되는 횟수 (개인=2, 일반=1 등) */
    @NotNull
    @Min(1)
    @Column(name = "deduction_count", nullable = false)
    private Integer deductionCount;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private LessonType(String name, Integer maxCapacity, Integer durationMinutes,
                       Integer deductionCount, boolean active) {
        this.name = name;
        this.maxCapacity = maxCapacity;
        this.durationMinutes = durationMinutes;
        this.deductionCount = deductionCount != null ? deductionCount : 1;
        this.active = active;
    }
}
