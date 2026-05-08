package com.pilates.domain.notification.entity;

import com.pilates.domain.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 알림 발송 이력 (카카오 알림톡 등).
 * 2차 MVP에서 구현 예정. 테이블 구조만 선확보.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_member_status", columnList = "member_id, status"),
        @Index(name = "idx_notifications_scheduled_status", columnList = "scheduled_at, status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notifications_member"))
    private Member member;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "template_code", length = 50)
    private String templateCode;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Notification(Member member, NotificationType type, String templateCode,
                         String content, NotificationStatus status, LocalDateTime scheduledAt) {
        this.member = member;
        this.type = type;
        this.templateCode = templateCode;
        this.content = content;
        this.status = status;
        this.scheduledAt = scheduledAt;
    }
}
