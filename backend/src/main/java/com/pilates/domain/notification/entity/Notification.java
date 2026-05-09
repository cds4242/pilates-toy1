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
 * 알림 발송 이력.
 * 카카오 알림톡 → SMS 폴백 흐름을 기록한다.
 * recipientType/recipientId로 회원·강사 등 다중 수신자 지원.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_member_status", columnList = "member_id, status"),
        @Index(name = "idx_notifications_scheduled_status", columnList = "scheduled_at, status"),
        @Index(name = "idx_notifications_recipient", columnList = "recipient_type, recipient_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 회원 수신자 (하위 호환용, 강사 알림은 null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",
            foreignKey = @ForeignKey(name = "fk_notifications_member"))
    private Member member;

    /** 수신자 유형 */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private RecipientType recipientType;

    /** 수신자 ID (recipientType에 따라 members.id 또는 instructors.id) */
    @Column(name = "recipient_id")
    private Long recipientId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private NotificationChannel channel;

    @Column(name = "message_id", length = 100)
    private String messageId;

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
    private Notification(Member member, RecipientType recipientType, Long recipientId,
                         NotificationType type, String templateCode,
                         String content, NotificationStatus status, LocalDateTime scheduledAt) {
        this.member = member;
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.type = type;
        this.templateCode = templateCode;
        this.content = content;
        this.status = status;
        this.scheduledAt = scheduledAt;
    }

    /** 회원 대상 알림 생성 */
    public static Notification createForMember(Member member, NotificationType type,
                                                String templateCode, String content,
                                                LocalDateTime scheduledAt) {
        return Notification.builder()
                .member(member)
                .recipientType(RecipientType.MEMBER)
                .recipientId(member.getId())
                .type(type)
                .templateCode(templateCode)
                .content(content)
                .status(NotificationStatus.PENDING)
                .scheduledAt(scheduledAt)
                .build();
    }

    /** 강사 대상 알림 생성 */
    public static Notification createForInstructor(Long instructorId, NotificationType type,
                                                    String templateCode, String content,
                                                    LocalDateTime scheduledAt) {
        return Notification.builder()
                .member(null)
                .recipientType(RecipientType.INSTRUCTOR)
                .recipientId(instructorId)
                .type(type)
                .templateCode(templateCode)
                .content(content)
                .status(NotificationStatus.PENDING)
                .scheduledAt(scheduledAt)
                .build();
    }

    /** @deprecated createForMember 사용 */
    @Deprecated
    public static Notification create(Member member, NotificationType type,
                                       String templateCode, String content,
                                       LocalDateTime scheduledAt) {
        return createForMember(member, type, templateCode, content, scheduledAt);
    }

    /** 알림톡 발송 성공 */
    public void markAsSent(NotificationChannel channel, String messageId) {
        this.status = NotificationStatus.SENT;
        this.channel = channel;
        this.messageId = messageId;
        this.sentAt = LocalDateTime.now();
    }

    /** 발송 실패 */
    public void markAsFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }

    /** SMS 폴백 발송 성공 */
    public void markAsFallbackSent(String messageId) {
        this.status = NotificationStatus.FALLBACK_SENT;
        this.channel = NotificationChannel.SMS;
        this.messageId = messageId;
        this.sentAt = LocalDateTime.now();
    }

    /** 재발송을 위해 PENDING으로 되돌리기 */
    public void resetForResend() {
        this.status = NotificationStatus.PENDING;
        this.failureReason = null;
        this.channel = null;
        this.messageId = null;
        this.sentAt = null;
    }

    /** 발송 준비 완료 여부 */
    public boolean isReadyToSend(LocalDateTime now) {
        return this.status == NotificationStatus.PENDING
                && (this.scheduledAt == null || !this.scheduledAt.isAfter(now));
    }
}
