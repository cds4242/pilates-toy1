-- =====================================================
-- V2: 인덱스 추가 (조회 패턴 기반)
-- FK 인덱스는 InnoDB가 자동 생성하지만 명시적으로 추가하는 것도 있음
-- =====================================================

-- ── 회원 ──
CREATE INDEX idx_members_status ON members(status, deleted_at);

-- ── 회원 메모 ──
CREATE INDEX idx_member_memos_member ON member_memos(member_id);

-- ── 강사 근무 가능 시간 ──
CREATE INDEX idx_instructor_avail_instructor ON instructor_available_times(instructor_id);

-- ── 고정 스케줄 ──
CREATE INDEX idx_fixed_schedules_instructor ON fixed_schedules(instructor_id);

-- ── 수업 시간표 ──
CREATE INDEX idx_class_schedules_date_instructor ON class_schedules(class_date, instructor_id);
CREATE INDEX idx_class_schedules_date_status ON class_schedules(class_date, status);

-- ── 정기권 ──
CREATE INDEX idx_memberships_member_status ON memberships(member_id, status);
CREATE INDEX idx_memberships_end_date ON memberships(end_date);

-- ── 정기권 홀딩 ──
CREATE INDEX idx_membership_holdings_membership ON membership_holdings(membership_id);

-- ── 탈퇴 회원 로그 (익명화 배치용) ──
CREATE INDEX idx_withdrawn_logs_member ON withdrawn_member_logs(member_id);
CREATE INDEX idx_withdrawn_logs_withdrawn_at ON withdrawn_member_logs(withdrawn_at);
CREATE INDEX idx_withdrawn_logs_anonymized ON withdrawn_member_logs(anonymized, anonymized_at);

-- ── 정기권-수업 유형 매핑 ──
CREATE INDEX idx_mlt_membership ON membership_lesson_types(membership_id);
CREATE INDEX idx_mlt_lesson_type ON membership_lesson_types(lesson_type_id);

-- ── 예약 ──
CREATE INDEX idx_reservations_schedule_status ON reservations(class_schedule_id, status);
CREATE INDEX idx_reservations_member_status ON reservations(member_id, status);
CREATE INDEX idx_reservations_member_status_created ON reservations(member_id, status, created_at);

-- ── 출석 ──
CREATE INDEX idx_attendances_member ON attendances(member_id);
CREATE INDEX idx_attendances_schedule ON attendances(class_schedule_id);

-- ── 결제 ──
CREATE INDEX idx_payments_member ON payments(member_id);
CREATE INDEX idx_payments_paid_at ON payments(paid_at);

-- ── 알림 ──
CREATE INDEX idx_notifications_member_status ON notifications(member_id, status);
CREATE INDEX idx_notifications_scheduled_status ON notifications(scheduled_at, status);

-- ── 감사 로그 ──
CREATE INDEX idx_audit_logs_admin ON admin_audit_logs(admin_id);
CREATE INDEX idx_audit_logs_created ON admin_audit_logs(created_at);
CREATE INDEX idx_audit_logs_target ON admin_audit_logs(target_type, target_id);
