package com.pilates.domain.membership.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 정기권 홀딩(일시정지) 이력.
 * 홀딩 기간만큼 정기권 종료일이 자동 연장된다.
 */
@Entity
@Table(name = "membership_holdings", indexes = {
        @Index(name = "idx_membership_holdings_membership", columnList = "membership_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_membership_holdings_membership"))
    private Membership membership;

    @NotNull
    @Column(name = "hold_start_date", nullable = false)
    private LocalDate holdStartDate;

    @Column(name = "hold_end_date")
    private LocalDate holdEndDate;

    @Column(name = "reason", length = 500)
    private String reason;

    /** 홀딩으로 연장된 일수 */
    @Column(name = "extended_days")
    private Integer extendedDays;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MembershipHolding(Membership membership, LocalDate holdStartDate,
                              LocalDate holdEndDate, String reason, Integer extendedDays) {
        this.membership = membership;
        this.holdStartDate = holdStartDate;
        this.holdEndDate = holdEndDate;
        this.reason = reason;
        this.extendedDays = extendedDays;
    }
}
