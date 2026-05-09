-- =====================================================
-- V9: 알림 템플릿 테이블 + notifications 채널/메시지ID 컬럼 추가
-- =====================================================

CREATE TABLE notification_templates (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)     NOT NULL,
    title       VARCHAR(100)    NOT NULL,
    body        TEXT            NOT NULL,
    channel     VARCHAR(20)     NOT NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (code),
    INDEX idx_template_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- notifications 테이블에 channel, message_id 컬럼 추가
ALTER TABLE notifications ADD COLUMN channel VARCHAR(20) NULL AFTER status;
ALTER TABLE notifications ADD COLUMN message_id VARCHAR(100) NULL AFTER channel;
