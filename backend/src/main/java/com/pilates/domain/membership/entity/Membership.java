package com.pilates.domain.membership.entity;

import com.pilates.common.entity.BaseEntity;
import com.pilates.domain.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 정기권(수강권) 엔티티.
 * 잔여 횟수 차감 시 비관적 락(@Lock(PESSIMISTIC_WRITE)) 사용 (Repository에서).
 * 무제한권은 is_unlimited = true, remaining_count 사용하지 않음.
 * 수업 유형 매핑은 membership_lesson_types 중간 테이블로 관리 (1:N).
 */
@Entity
@Table(name = "memberships", indexes = {
        @Index(name = "idx_memberships_member_status", columnList = "member_id, status"),
        @Index(name = "idx_memberships_end_date", columnList = "end_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 32)
    private String publicId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_memberships_member"))
    private Member member;

    /** 총 횟수 (무제한권이면 의미 없음) */
    @NotNull
    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    /** 잔여 횟수 (무제한권이면 사용하지 않음) */
    @NotNull
    @Column(name = "remaining_count", nullable = false)
    private Integer remainingCount;

    /** 무제한권 여부. 무제한이면 월 한도는 studio_settings로 관리. */
    @Column(name = "is_unlimited", nullable = false)
    private boolean unlimited;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** 금액. DECIMAL(10,0) — 원화는 소수점 없음. */
    @NotNull
    @Column(name = "price", nullable = false, precision = 10, scale = 0)
    private BigDecimal price;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MembershipStatus status;

    @Builder
    private Membership(String publicId, Member member,
                       Integer totalCount, Integer remainingCount, boolean unlimited,
                       LocalDate startDate, LocalDate endDate, BigDecimal price,
                       MembershipStatus status) {
        this.publicId = publicId;
        this.member = member;
        this.totalCount = totalCount;
        this.remainingCount = remainingCount;
        this.unlimited = unlimited;
        this.startDate = startDate;
        this.endDate = endDate;
        this.price = price;
        this.status = status;
    }

    // ── 도메인 메서드 ──

    /** 횟수 차감. 무제한권은 차감 X. 잔여 0이면 EXHAUSTED. */
    public void deduct(int count) {
        if (this.unlimited) return;
        if (this.remainingCount < count) {
            throw new IllegalStateException("잔여 횟수 부족. 잔여=" + this.remainingCount + ", 차감=" + count);
        }
        this.remainingCount -= count;
        if (this.remainingCount == 0) {
            this.status = MembershipStatus.EXHAUSTED;
        }
    }

    /** 횟수 복구 (예약 취소 시). EXHAUSTED → ACTIVE 전환 가능. */
    public void restore(int count) {
        if (this.unlimited) return;
        this.remainingCount += count;
        if (this.status == MembershipStatus.EXHAUSTED && this.remainingCount > 0) {
            this.status = MembershipStatus.ACTIVE;
        }
    }

    /** 홀딩 시작. ACTIVE에서만 가능. */
    public void startHolding() {
        if (this.status != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태만 일시정지 가능. 현재: " + this.status);
        }
        this.status = MembershipStatus.HOLDING;
    }

    /** 홀딩 해제 + 유효기간 연장. */
    public void endHolding(int extendedDays) {
        if (this.status != MembershipStatus.HOLDING) {
            throw new IllegalStateException("일시정지 상태만 해제 가능. 현재: " + this.status);
        }
        this.status = MembershipStatus.ACTIVE;
        this.endDate = this.endDate.plusDays(extendedDays);
    }

    /** 만료 처리 (스케줄러). */
    public void expire() {
        this.status = MembershipStatus.EXPIRED;
    }

    /** 사용 가능 여부 (활성 + 유효기간 내). */
    public boolean isUsable(LocalDate today) {
        return this.status == MembershipStatus.ACTIVE && !this.endDate.isBefore(today);
    }

    /** 소진 여부. */
    public boolean isExhausted() {
        return this.status == MembershipStatus.EXHAUSTED;
    }
}
