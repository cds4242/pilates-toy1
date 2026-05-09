package com.pilates.domain.reservation.entity;

import com.pilates.common.entity.BaseEntity;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.membership.entity.Membership;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 엔티티.
 * 중복 예약 방지: 비즈니스 로직에서 CONFIRMED/WAITING 상태만 체크.
 * DB UNIQUE 제약은 걸지 않음 (취소 후 재예약 허용).
 */
@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservations_schedule_status", columnList = "class_schedule_id, status"),
        @Index(name = "idx_reservations_member_status", columnList = "member_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_member"))
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_class_schedule"))
    private ClassSchedule classSchedule;

    /** 사용된 정기권 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_membership"))
    private Membership membership;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    /** 대기 순번 (NULL이면 확정 예약) */
    @Column(name = "wait_order")
    private Integer waitOrder;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private java.time.LocalDateTime cancelledAt;

    @Builder
    private Reservation(Member member, ClassSchedule classSchedule, Membership membership,
                        ReservationStatus status, Integer waitOrder) {
        this.member = member;
        this.classSchedule = classSchedule;
        this.membership = membership;
        this.status = status;
        this.waitOrder = waitOrder;
    }

    // ── 도메인 메서드 ──

    /** 예약 취소. CONFIRMED → CANCELLED. */
    public void cancel(String reason) {
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("확정 상태만 취소 가능. 현재: " + this.status);
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = java.time.LocalDateTime.now();
    }

    /** 노쇼 처리. CONFIRMED → NO_SHOW. */
    public void markNoShow() {
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("확정 상태만 노쇼 가능. 현재: " + this.status);
        }
        this.status = ReservationStatus.NO_SHOW;
    }

    /** 대기 → 확정 승격 (v2). */
    public void promote() {
        if (this.status != ReservationStatus.WAITING) {
            throw new IllegalStateException("대기 상태만 승격 가능. 현재: " + this.status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.waitOrder = null;
    }

    /** 취소 가능 여부 (수업 시작 N시간 전까지). */
    public boolean canCancel(java.time.LocalDateTime now, int deadlineHours) {
        if (this.status != ReservationStatus.CONFIRMED) return false;
        java.time.LocalDateTime classStart = this.classSchedule.getClassDate()
                .atTime(this.classSchedule.getStartTime());
        return now.isBefore(classStart.minusHours(deadlineHours));
    }

    /** 하위 호환용 (기본 2시간) */
    public boolean canCancel(java.time.LocalDateTime now) {
        return canCancel(now, 2);
    }

    /** 미래 수업인지. */
    public boolean isUpcoming(java.time.LocalDateTime now) {
        java.time.LocalDateTime classStart = this.classSchedule.getClassDate()
                .atTime(this.classSchedule.getStartTime());
        return now.isBefore(classStart);
    }
}
