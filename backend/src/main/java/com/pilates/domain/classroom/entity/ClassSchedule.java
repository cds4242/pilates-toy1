package com.pilates.domain.classroom.entity;

import com.pilates.domain.instructor.entity.Instructor;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 실제 수업 시간표 (단건 + 반복 생성분).
 * current_count + @Version으로 정원 관리 (낙관적 락).
 */
@Entity
@Table(name = "class_schedules", indexes = {
        @Index(name = "idx_class_schedules_date_instructor", columnList = "class_date, instructor_id"),
        @Index(name = "idx_class_schedules_date_status", columnList = "class_date, status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_class_schedules_instructor"))
    private Instructor instructor;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_class_schedules_lesson_type"))
    private LessonType lessonType;

    /** 반복 생성이면 원본 고정 스케줄 참조, 단건이면 NULL */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_schedule_id",
            foreignKey = @ForeignKey(name = "fk_class_schedules_fixed_schedule"))
    private FixedSchedule fixedSchedule;

    @NotNull
    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @NotNull
    @Min(1)
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    /** 현재 예약 인원 (비정규화, 낙관적 락으로 동시성 제어) */
    @NotNull
    @Min(0)
    @Column(name = "current_count", nullable = false)
    private Integer currentCount;

    /** 낙관적 락 버전 */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClassScheduleStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ClassSchedule(Instructor instructor, LessonType lessonType, FixedSchedule fixedSchedule,
                          LocalDate classDate, LocalTime startTime, LocalTime endTime,
                          Integer maxCapacity, ClassScheduleStatus status) {
        this.instructor = instructor;
        this.lessonType = lessonType;
        this.fixedSchedule = fixedSchedule;
        this.classDate = classDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCapacity = maxCapacity;
        this.currentCount = 0;
        this.status = status;
    }

    // ── 도메인 메서드 ──

    /** 고정 스케줄 기반 수업 생성 (팩토리) */
    public static ClassSchedule createFromFixed(FixedSchedule fixed, LocalDate classDate) {
        return ClassSchedule.builder()
                .instructor(fixed.getInstructor())
                .lessonType(fixed.getLessonType())
                .fixedSchedule(fixed)
                .classDate(classDate)
                .startTime(fixed.getStartTime())
                .endTime(fixed.getEndTime())
                .maxCapacity(fixed.getLessonType().getMaxCapacity())
                .status(ClassScheduleStatus.SCHEDULED)
                .build();
    }

    /** 예약 인원 증가. 정원 초과 시 IllegalStateException. */
    public void incrementCount() {
        if (this.currentCount >= this.maxCapacity) {
            throw new IllegalStateException("정원이 가득 찼습니다.");
        }
        this.currentCount++;
    }

    /** 예약 인원 감소. 0 미만 방지. */
    public void decrementCount() {
        if (this.currentCount <= 0) {
            throw new IllegalStateException("예약 인원이 0입니다.");
        }
        this.currentCount--;
    }

    /** 수업 취소 (휴강). SCHEDULED 상태에서만 가능. */
    public void cancel() {
        if (this.status != ClassScheduleStatus.SCHEDULED) {
            throw new IllegalStateException("예정 상태의 수업만 취소할 수 있습니다. 현재: " + this.status);
        }
        this.status = ClassScheduleStatus.CANCELLED;
    }

    /** 수업 완료 처리. SCHEDULED 상태에서만 가능. */
    public void complete() {
        if (this.status != ClassScheduleStatus.SCHEDULED) {
            throw new IllegalStateException("예정 상태의 수업만 완료할 수 있습니다. 현재: " + this.status);
        }
        this.status = ClassScheduleStatus.COMPLETED;
    }

    /** 예약 가능 여부 */
    public boolean isReservable() {
        return this.status == ClassScheduleStatus.SCHEDULED
                && this.currentCount < this.maxCapacity
                && !this.classDate.isBefore(LocalDate.now());
    }

    /** 취소 가능 여부 */
    public boolean isCancellable() {
        return this.status == ClassScheduleStatus.SCHEDULED;
    }
}
