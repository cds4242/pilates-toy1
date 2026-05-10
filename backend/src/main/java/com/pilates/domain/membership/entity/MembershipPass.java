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
import java.time.LocalDate;

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

    /** 회원 노출 여부 */
    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    /** 판매 활성 상태 */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** 판매 시작일 (null이면 즉시 판매) */
    @Column(name = "sale_start_date")
    private LocalDate saleStartDate;

    /** 판매 종료일 (null이면 무기한) */
    @Column(name = "sale_end_date")
    private LocalDate saleEndDate;

    /** 카테고리 (PERSONAL/GROUP/UNLIMITED) */
    @Column(name = "category", length = 20)
    private String category;

    /** 상품 설명 */
    @Column(name = "description", length = 500)
    private String description;

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

    /** 확장 정보 수정 */
    public void updateExtendedInfo(Boolean visible, Boolean active, LocalDate saleStartDate,
                                    LocalDate saleEndDate, String category, String description) {
        if (visible != null) this.visible = visible;
        if (active != null) this.active = active;
        if (saleStartDate != null) this.saleStartDate = saleStartDate;
        this.saleEndDate = saleEndDate; // null 허용 (무기한)
        if (category != null) this.category = category;
        if (description != null) this.description = description;
    }

    /** 비활성화 (판매 중지) — 이미 발급된 회원은 계속 사용 가능 */
    public void deactivatePass() {
        this.active = false;
    }

    /** 활성화 (판매 재개) */
    public void activatePass() {
        this.active = true;
    }

    /** 현재 판매 가능한 상태인지 (노출 + 활성 + 판매 기간 내) */
    public boolean isSellable(LocalDate today) {
        if (!this.visible || !this.active) return false;
        if (this.saleStartDate != null && today.isBefore(this.saleStartDate)) return false;
        if (this.saleEndDate != null && today.isAfter(this.saleEndDate)) return false;
        return true;
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
