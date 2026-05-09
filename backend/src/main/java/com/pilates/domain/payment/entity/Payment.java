package com.pilates.domain.payment.entity;

import com.pilates.domain.member.entity.Member;
import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipPass;
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
 * order_id UNIQUE로 멱등성 보장.
 * 결제 흐름: PENDING → COMPLETED → (REFUNDED/PARTIAL_REFUND)
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

    /** 결제 고유 번호 (멱등성 핵심, UNIQUE) */
    @NotNull
    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    /** 토스 paymentKey (승인 후 저장) */
    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_member"))
    private Member member;

    /** 구매한 정기권 종류 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_pass_id",
            foreignKey = @ForeignKey(name = "fk_payments_membership_pass"))
    private MembershipPass membershipPass;

    /** 발급된 회원 정기권 (결제 성공 후 채움) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id",
            foreignKey = @ForeignKey(name = "fk_payments_membership"))
    private Membership membership;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 10, scale = 0)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "payment_method_detail", length = 100)
    private String paymentMethodDetail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "refund_amount", precision = 10, scale = 0)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Payment(String orderId, Member member, MembershipPass membershipPass,
                    BigDecimal amount, PaymentMethod method, PaymentStatus status) {
        this.orderId = orderId;
        this.member = member;
        this.membershipPass = membershipPass;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.refundAmount = BigDecimal.ZERO;
    }

    /** 결제 승인 완료. PENDING → COMPLETED. */
    public void confirm(String paymentKey, String methodDetail, LocalDateTime paidAt) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 결제만 승인 가능. 현재: " + this.status);
        }
        this.paymentKey = paymentKey;
        this.paymentMethodDetail = methodDetail;
        this.paidAt = paidAt;
        this.status = PaymentStatus.COMPLETED;
    }

    /** 결제 실패. PENDING → FAILED. */
    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.refundReason = reason;
    }

    /** 발급된 정기권 연결. */
    public void linkMembership(Membership membership) {
        this.membership = membership;
    }

    /** 환불 처리. 누적 가능. */
    public void refund(BigDecimal refundAmt, String reason) {
        if (!isRefundable()) {
            throw new IllegalStateException("환불 불가 상태. 현재: " + this.status);
        }
        BigDecimal newTotal = (this.refundAmount != null ? this.refundAmount : BigDecimal.ZERO).add(refundAmt);
        if (newTotal.compareTo(this.amount) > 0) {
            throw new IllegalStateException("환불 금액 초과");
        }
        this.refundAmount = newTotal;
        this.refundReason = reason;
        this.refundedAt = LocalDateTime.now();
        this.status = newTotal.compareTo(this.amount) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIAL_REFUND;
    }

    public boolean isRefundable() {
        return this.status == PaymentStatus.COMPLETED || this.status == PaymentStatus.PARTIAL_REFUND;
    }

    public BigDecimal getRefundableAmount() {
        return this.amount.subtract(this.refundAmount != null ? this.refundAmount : BigDecimal.ZERO);
    }
}
