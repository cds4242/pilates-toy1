package com.pilates.domain.notification.repository;

import com.pilates.domain.notification.entity.Notification;
import com.pilates.domain.notification.entity.NotificationStatus;
import com.pilates.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 Repository.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    List<Notification> findAllByStatusAndScheduledAtBefore(NotificationStatus status, LocalDateTime before);

    List<Notification> findAllByStatus(NotificationStatus status);

    boolean existsByMemberIdAndTypeAndTemplateCode(Long memberId, NotificationType type, String templateCode);

    @Query("SELECT n FROM Notification n WHERE n.member.id = :memberId AND n.type = :type " +
            "AND n.templateCode = :templateCode AND n.createdAt > :after")
    List<Notification> findRecentByMemberAndType(@Param("memberId") Long memberId,
                                                  @Param("type") NotificationType type,
                                                  @Param("templateCode") String templateCode,
                                                  @Param("after") LocalDateTime after);

    /** 관리자: 전체 알림 목록 (필터) */
    @Query("SELECT n FROM Notification n WHERE " +
            "(:memberId IS NULL OR n.member.id = :memberId) AND " +
            "(:status IS NULL OR n.status = :status) AND " +
            "(:type IS NULL OR n.type = :type) AND " +
            "(:from IS NULL OR n.createdAt >= :from) AND " +
            "(:to IS NULL OR n.createdAt <= :to) " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findAllWithFilters(@Param("memberId") Long memberId,
                                          @Param("status") NotificationStatus status,
                                          @Param("type") NotificationType type,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          Pageable pageable);

    /** 통계: 상태별 카운트 */
    long countByStatus(NotificationStatus status);

    /** 통계: 기간별 상태 카운트 */
    long countByStatusAndCreatedAtBetween(NotificationStatus status, LocalDateTime from, LocalDateTime to);
}
