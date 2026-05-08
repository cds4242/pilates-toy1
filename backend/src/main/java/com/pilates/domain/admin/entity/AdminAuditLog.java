package com.pilates.domain.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 관리자 행위 감사 로그.
 * 누가, 무엇을, 언제 변경했는지 기록.
 * 회원 정보 수정, 정기권 발급/수정, 예약 변경 등 주요 행위 추적.
 */
@Entity
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_audit_logs_admin", columnList = "admin_id"),
        @Index(name = "idx_audit_logs_created", columnList = "created_at"),
        @Index(name = "idx_audit_logs_target", columnList = "target_type, target_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 행위자 */
    @NotNull
    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    /** 행위 (CREATE, UPDATE, DELETE, LOGIN 등) */
    @NotBlank
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    /** 대상 엔티티 타입 (MEMBER, MEMBERSHIP, RESERVATION 등) */
    @NotBlank
    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    /** 대상 엔티티 ID */
    @Column(name = "target_id")
    private Long targetId;

    /** 변경 내용 상세 (JSON) */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** 요청 IP */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private AdminAuditLog(Long adminId, String action, String targetType,
                          Long targetId, String detail, String ipAddress) {
        this.adminId = adminId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
        this.ipAddress = ipAddress;
    }
}
