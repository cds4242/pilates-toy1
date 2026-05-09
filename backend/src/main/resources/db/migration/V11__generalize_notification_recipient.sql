-- =====================================================
-- V11: notifications 수신자 일반화
-- member_id FK 전용 → recipient_type + recipient_id 다중 수신자 지원
-- =====================================================

-- member_id NULL 허용 (강사 알림 등 member가 아닌 수신자)
ALTER TABLE notifications MODIFY COLUMN member_id BIGINT NULL;

-- 수신자 타입/ID 컬럼 추가
ALTER TABLE notifications
    ADD COLUMN recipient_type VARCHAR(20) NOT NULL DEFAULT 'MEMBER' AFTER member_id,
    ADD COLUMN recipient_id BIGINT NULL AFTER recipient_type;

CREATE INDEX idx_notifications_recipient ON notifications(recipient_type, recipient_id);

-- 기존 데이터: member_id → recipient_id로 복사
UPDATE notifications SET recipient_id = member_id WHERE recipient_id IS NULL AND member_id IS NOT NULL;
