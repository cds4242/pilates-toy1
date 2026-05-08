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
}
