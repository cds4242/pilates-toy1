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
    ('dev_instructor_001', '박지영', '010-1111-2222', 'ACTIVE'),
    ('dev_instructor_002', '이수진', '010-3333-4444', 'ACTIVE'),
    ('dev_instructor_003', '최재훈', '010-5555-6666', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ── 강사 근무 가능 시간 ──
-- 박지영: 월~금 09:00~18:00
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time) VALUES
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_001'), 'MONDAY', '09:00:00', '18:00:00'),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_001'), 'TUESDAY', '09:00:00', '18:00:00'),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_001'), 'WEDNESDAY', '09:00:00', '18:00:00'),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_001'), 'THURSDAY', '09:00:00', '18:00:00'),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_001'), 'FRIDAY', '09:00:00', '18:00:00')
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time);

-- 이수진: 월수금 10:00~19:00
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time) VALUES
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_002'), 'MONDAY', '10:00:00', '19:00:00'),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_002'), 'WEDNESDAY', '10:00:00', '19:00:00'),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_002'), 'FRIDAY', '10:00:00', '19:00:00')
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time);

-- 최재훈: 화목토 09:00~17:00
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time) VALUES
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_003'), 'TUESDAY', '09:00:00', '17:00:00'),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_003'), 'THURSDAY', '09:00:00', '17:00:00'),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_003'), 'SATURDAY', '09:00:00', '17:00:00')
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time);

-- ── 고정 스케줄 예시 ──
-- 박지영: 월 10:00 개인, 월 11:00 듀엣, 수 14:00 그룹
INSERT INTO fixed_schedules (instructor_id, lesson_type_id, day_of_week, start_time, end_time, is_active) VALUES
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_001'),
     (SELECT id FROM lesson_types WHERE name = '개인'), 'MONDAY', '10:00:00', '10:50:00', 1),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_001'),
     (SELECT id FROM lesson_types WHERE name = '듀엣'), 'MONDAY', '11:00:00', '11:50:00', 1),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_001'),
     (SELECT id FROM lesson_types WHERE name = '그룹'), 'WEDNESDAY', '14:00:00', '14:50:00', 1)
ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);

-- 이수진: 수 10:00 개인, 금 15:00 듀엣
INSERT INTO fixed_schedules (instructor_id, lesson_type_id, day_of_week, start_time, end_time, is_active) VALUES
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_002'),
     (SELECT id FROM lesson_types WHERE name = '개인'), 'WEDNESDAY', '10:00:00', '10:50:00', 1),
    ((SELECT id FROM instructors WHERE public_id = 'dev_instructor_002'),
     (SELECT id FROM lesson_types WHERE name = '듀엣'), 'FRIDAY', '15:00:00', '15:50:00', 1)
ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);

-- ── 공휴일 ──
INSERT INTO holidays (holiday_date, name) VALUES
    ('2026-08-15', '광복절'),
    ('2026-10-03', '개천절')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ── 개발용 정기권 (회원 가입 후 사용 가능) ──
-- 회원이 존재하는 경우에만 삽입. 개발 테스트 시 회원 가입 후 아래 쿼리를 직접 실행하거나,
-- Admin API(POST /api/admin/memberships)로 발급한다.
-- 예시: 12회권, 그룹+듀엣, 480000원, 2026-01-15 ~ 2026-07-30
--
-- INSERT INTO memberships (public_id, member_id, total_count, remaining_count, is_unlimited, start_date, end_date, price, status)
-- VALUES ('dev_membership_001', (SELECT id FROM members WHERE deleted_at IS NULL LIMIT 1),
--         12, 8, 0, '2026-01-15', '2026-07-30', 480000, 'ACTIVE')
-- ON DUPLICATE KEY UPDATE status = VALUES(status);
--
-- INSERT INTO membership_lesson_types (membership_id, lesson_type_id) VALUES
--     ((SELECT id FROM memberships WHERE public_id = 'dev_membership_001'),
--      (SELECT id FROM lesson_types WHERE name = '그룹')),
--     ((SELECT id FROM memberships WHERE public_id = 'dev_membership_001'),
--      (SELECT id FROM lesson_types WHERE name = '듀엣'))
-- ON DUPLICATE KEY UPDATE lesson_type_id = VALUES(lesson_type_id);
