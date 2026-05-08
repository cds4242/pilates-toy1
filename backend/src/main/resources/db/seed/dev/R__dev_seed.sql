-- =====================================================
-- Flyway Repeatable Migration: 개발 시드 데이터
-- 매 실행 시 내용이 변경되면 재적용됨
-- 운영 환경에서는 절대 실행되지 않음 (flyway.locations 분기)
-- =====================================================

-- ── 수업 유형 기본값 ──
INSERT INTO lesson_types (name, max_capacity, duration_minutes, deduction_count, is_active) VALUES
    ('개인', 1, 50, 2, 1),
    ('듀엣', 2, 50, 1, 1),
    ('그룹', 8, 50, 1, 1),
    ('체험', 1, 50, 0, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ── 스튜디오 기본 설정 ──
INSERT INTO studio_settings (setting_key, setting_value, description) VALUES
    ('CANCEL_DEADLINE_HOURS', '2', '무료 취소 가능 시간 (수업 시작 N시간 전)'),
    ('UNLIMITED_MONTHLY_LIMIT', '30', '무제한권 월 최대 이용 횟수'),
    ('DEFAULT_LESSON_DURATION', '50', '기본 수업 시간 (분)'),
    ('NO_SHOW_AUTO_MARK_MINUTES', '30', '수업 종료 후 N분 뒤 미출석 자동 노쇼 처리'),
    ('REMINDER_1DAY_HOUR', '20', '전날 리마인더 발송 시각 (시)'),
    ('REMINDER_SAME_DAY_HOURS', '2', '당일 리마인더 (수업 N시간 전)'),
    ('MEMBERSHIP_EXPIRY_ALERT_DAYS', '7,3', '정기권 만료 알림 (D-N일)'),
    ('MEMBERSHIP_LOW_COUNT_ALERT', '3', '정기권 잔여 횟수 부족 알림 기준')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- ── 개발용 관리자 계정 (비밀번호: admin1234 → BCrypt 해시) ──
INSERT INTO admins (login_id, password_hash, name, role, is_active) VALUES
    ('admin', '$2a$10$dXJ3SW6G7P50lGmMQoeKhOelZJ2FG.VZL3ug0fE4ypyBHzDhFOKSS', '시스템관리자', 'SUPER_ADMIN', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ── 개발용 강사 ──
INSERT INTO instructors (public_id, name, phone, status) VALUES
    ('dev_instructor_001', '김강사', '010-1234-5678', 'ACTIVE'),
    ('dev_instructor_002', '이강사', '010-2345-6789', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ── 개발용 정기권-수업 유형 매핑 예시 ──
-- 실제 정기권 데이터가 있어야 하므로 주석 처리. 도메인 개발 후 활성화.
-- INSERT INTO membership_lesson_types (membership_id, lesson_type_id) VALUES
--     (1, 3);  -- 예: 1번 정기권 → 그룹 수업
