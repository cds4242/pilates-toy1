package com.pilates.domain.membership.entity;

import com.pilates.common.entity.BaseEntity;
import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 정기권 종류(상품 카탈로그) 엔티티.
 * 관리자가 등록하는 정기권 템플릿으로, 실제 발급 시 이 정보를 기반으로 Membership을 생성한다.
 */
@Entity
@Table(name = "membership_pass")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipPass extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 32)
    private String publicId;

    @NotBlank
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Column(name = "price", nullable = false, precision = 10, scale = 0)
    private BigDecimal price;

    /** 총 횟수 (무제한권이면 null) */
    @Column(name = "total_count")
    private Integer totalCount;

    /** 유효 기간 (일) */
    @Column(name = "validity_days", nullable = false)
    private int validityDays;

    /** 무제한권 여부 */
    @Column(name = "is_unlimited", nullable = false)
    private boolean unlimited;

    /** 무제한권 월 최대 이용 횟수 (횟수제이면 null) */
    @Column(name = "monthly_limit")
    private Integer monthlyLimit;

    /** 표시 순서 */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder
    private MembershipPass(String publicId, String name, BigDecimal price,
                           Integer totalCount, int validityDays, boolean unlimited,
                           Integer monthlyLimit, int displayOrder) {
        this.publicId = publicId;
        this.name = name;
        this.price = price;
        this.totalCount = totalCount;
        this.validityDays = validityDays;
        this.unlimited = unlimited;
        this.monthlyLimit = monthlyLimit;
        this.displayOrder = displayOrder;
    }

    /**
     * 정기권 종류 정보 수정. null이 아닌 필드만 업데이트.
     */
    public void updateInfo(String name, BigDecimal price, Integer totalCount,
                           Integer validityDays, Integer displayOrder) {
        if (name != null && !name.isBlank()) this.name = name;
        if (price != null) this.price = price;
        if (totalCount != null) this.totalCount = totalCount;
        if (validityDays != null) this.validityDays = validityDays;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }

    /**
     * 설정 유효성 검증.
     * 무제한권: totalCount는 null이어야 하고 monthlyLimit >= 1이어야 한다.
     * 횟수제: totalCount >= 1이어야 하고 monthlyLimit는 null이어야 한다.
     */
    public void validate() {
        if (this.unlimited) {
            if (this.totalCount != null || this.monthlyLimit == null || this.monthlyLimit < 1) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PASS_INVALID_CONFIG);
            }
        } else {
            if (this.totalCount == null || this.totalCount < 1 || this.monthlyLimit != null) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PASS_INVALID_CONFIG);
            }
        }
    }
}
