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

    // ── 도메인 메서드 (placeholder) ──

    /** 예약 취소 */
    public void cancel(String reason) {
        // TODO: 상태 검증 + 취소 + 정기권 복구 판단
    }

    /** 노쇼 처리 */
    public void markNoShow() {
        // TODO: 상태 → NO_SHOW
    }

    /** 대기 → 확정 승격 */
    public void promote() {
        // TODO: WAITING → CONFIRMED, waitOrder = null
    }
}
