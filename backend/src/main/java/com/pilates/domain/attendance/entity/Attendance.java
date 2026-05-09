package com.pilates.domain.attendance.entity;

import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.reservation.entity.Reservation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 출석 기록.
 * 예약 1건당 출석 기록 1건 (reservation_id UNIQUE).
 */
@Entity
@Table(name = "attendances", indexes = {
        @Index(name = "idx_attendances_member", columnList = "member_id"),
        @Index(name = "idx_attendances_schedule", columnList = "class_schedule_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 예약 건. 1:1 관계 (UNIQUE). 단방향. */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_attendances_reservation"))
    private Reservation reservation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_attendances_member"))
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_attendances_class_schedule"))
    private ClassSchedule classSchedule;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    /** 출석 체크 시각 */
    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    /** 체크한 관리자/강사 ID */
    @Column(name = "checked_by")
    private Long checkedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Attendance(Reservation reservation, Member member, ClassSchedule classSchedule,
                       AttendanceStatus status, LocalDateTime checkedAt, Long checkedBy) {
        this.reservation = reservation;
        this.member = member;
        this.classSchedule = classSchedule;
        this.status = status;
        this.checkedAt = checkedAt;
        this.checkedBy = checkedBy;
    }

    // ── 정적 팩토리 ──

    /** 예약 생성 시 PENDING 상태로 출석 기록 생성 */
    public static Attendance createPending(Reservation reservation) {
        return Attendance.builder()
                .reservation(reservation)
                .member(reservation.getMember())
                .classSchedule(reservation.getClassSchedule())
                .status(AttendanceStatus.PENDING)
                .build();
    }

    // ── 도메인 메서드 ──

    /** 출석 마킹 (강사) */
    public void markAttended(Long checkedBy) {
        this.status = AttendanceStatus.ATTENDED;
        this.checkedAt = LocalDateTime.now();
        this.checkedBy = checkedBy;
    }

    /** 지각 마킹 (강사) */
    public void markLate(Long checkedBy) {
        this.status = AttendanceStatus.LATE;
        this.checkedAt = LocalDateTime.now();
        this.checkedBy = checkedBy;
    }

    /** 결석 마킹 (강사) */
    public void markAbsent(Long checkedBy) {
        this.status = AttendanceStatus.ABSENT;
        this.checkedAt = LocalDateTime.now();
        this.checkedBy = checkedBy;
    }

    /** 노쇼 마킹 (스케줄러) */
    public void markNoShow() {
        this.status = AttendanceStatus.NO_SHOW;
    }

    /**
     * 출석 체크 가능 여부.
     * 수업 시작 시각 ~ 종료 N분 후 사이만 가능.
     */
    public boolean isCheckable(LocalDateTime now, int afterEndMinutes) {
        LocalDateTime classStart = classSchedule.getClassDate()
                .atTime(classSchedule.getStartTime());
        LocalDateTime classEndPlus = classSchedule.getClassDate()
                .atTime(classSchedule.getEndTime())
                .plusMinutes(afterEndMinutes);
        return !now.isBefore(classStart) && !now.isAfter(classEndPlus);
    }

    /** 하위 호환 (기본 30분) */
    public boolean isCheckable(LocalDateTime now) {
        return isCheckable(now, 30);
    }

    /** 강사가 이미 출석/지각/결석 마킹한 상태인지 */
    public boolean isManuallyChecked() {
        return status == AttendanceStatus.ATTENDED
                || status == AttendanceStatus.LATE
                || status == AttendanceStatus.ABSENT;
    }
}
