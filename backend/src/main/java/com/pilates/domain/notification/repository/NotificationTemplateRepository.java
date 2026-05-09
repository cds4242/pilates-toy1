package com.pilates.domain.notification.repository;

import com.pilates.domain.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 알림 템플릿 Repository.
 */
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByCodeAndDeletedAtIsNull(String code);
}
