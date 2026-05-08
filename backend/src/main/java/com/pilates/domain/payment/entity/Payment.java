package com.pilates.domain.payment.entity;

import com.pilates.domain.member.entity.Member;
import com.pilates.domain.membership.entity.Membership;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 엔티티.
 * order_id UNIQUE로 중복 결제 방지.
 * 금액은 DECIMAL(10,0) — 원화 기준 소수점 없음.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_member", columnList = "member_id"),
        @Index(name = "idx_payments_paid_at", columnList = "paid_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 결제 고유 번호 (중복 결제 방지 UNIQUE) */
    @NotNull
    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_member"))
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_membership"))
    private Membership membership;

    /** 결제 금액. DECIMAL(10,0). */
    @NotNull
    @Column(name = "amount", nullable = false, precision = 10, scale = 0)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /** 환불 금액 */
    @Column(name = "refund_amount", precision = 10, scale = 0)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Payment(String orderId, Member member, Membership membership,
                    BigDecimal amount, PaymentMethod method, PaymentStatus status,
                    LocalDateTime paidAt) {
        this.orderId = orderId;
        this.member = member;
        this.membership = membership;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.paidAt = paidAt;
    }

    // ── 도메인 메서드 (placeholder) ──

    /** 환불 처리 */
    public void refund(BigDecimal refundAmount, String reason) {
        // TODO: 환불
    }
}
