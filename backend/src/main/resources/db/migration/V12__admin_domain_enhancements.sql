-- ── STEP 11: admin 도메인 확장 ──

-- member_memos: admin_id 추가 (관리자도 메모 작성 가능), instructor_id nullable, soft delete
ALTER TABLE member_memos
    ADD COLUMN admin_id BIGINT NULL AFTER instructor_id,
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER updated_at,
    MODIFY COLUMN instructor_id BIGINT NULL;

ALTER TABLE member_memos
    ADD CONSTRAINT fk_member_memos_admin
        FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE RESTRICT;

CREATE INDEX idx_member_memos_member_deleted ON member_memos (member_id, deleted_at);

-- 통계 쿼리 성능 인덱스
CREATE INDEX idx_payments_paid_at_status ON payments (paid_at, status);
CREATE INDEX idx_payments_status_paid_at ON payments (status, paid_at);
CREATE INDEX idx_memberships_end_date_status ON memberships (end_date, status);
CREATE INDEX idx_members_status_created ON members (status, created_at);
CREATE INDEX idx_members_name ON members (name(100));
CREATE INDEX idx_reservations_created_at ON reservations (created_at);
