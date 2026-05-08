package com.pilates.domain.classroom.entity;

import com.pilates.domain.instructor.entity.Instructor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 주간 고정 반복 스케줄 템플릿.
 * 이 템플릿을 기반으로 매주 class_schedules가 자동 생성된다.
 */
@Entity
@Table(name = "fixed_schedules", indexes = {
        @Index(name = "idx_fixed_schedules_instructor", columnList = "instructor_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fixed_schedules_instructor"))
    private Instructor instructor;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fixed_schedules_lesson_type"))
    private LessonType lessonType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private FixedSchedule(Instructor instructor, LessonType lessonType, DayOfWeek dayOfWeek,
                          LocalTime startTime, LocalTime endTime, boolean active) {
        this.instructor = instructor;
        this.lessonType = lessonType;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
    }
}
