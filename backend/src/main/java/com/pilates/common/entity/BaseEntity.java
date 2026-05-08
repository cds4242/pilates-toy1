package com.pilates.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA 공통 베이스 엔티티.
 * 모든 엔티티가 상속하여 id, 감사(audit) 필드를 자동 관리한다.
 * Auditing: @CreatedDate / @LastModifiedDate (Spring Data JPA 표준) — @CreationTimestamp 사용 금지.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    /** 논리 삭제 일시. null이면 활성 데이터. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 논리 삭제 처리 */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /** 삭제 여부 확인 */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
