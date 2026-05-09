-- =====================================================
-- Flyway Repeatable Migration: 개발 시드 데이터
-- 매 실행 시 내용이 변경되면 재적용됨
-- 운영 환경에서는 절대 실행되지 않음 (flyway.locations 분기)
-- =====================================================

-- ── 수업 유형 기본값 (이미 존재하면 건너뜀) ──
INSERT INTO lesson_types (name, max_capacity, duration_minutes, deduction_count, is_active)
SELECT '개인', 1, 50, 2, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM lesson_types WHERE name = '개인');
INSERT INTO lesson_types (name, max_capacity, duration_minutes, deduction_count, is_active)
SELECT '듀엣', 2, 50, 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM lesson_types WHERE name = '듀엣');
INSERT INTO lesson_types (name, max_capacity, duration_minutes, deduction_count, is_active)
SELECT '그룹', 8, 50, 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM lesson_types WHERE name = '그룹');
INSERT INTO lesson_types (name, max_capacity, duration_minutes, deduction_count, is_active)
SELECT '체험', 1, 50, 0, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM lesson_types WHERE name = '체험');

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
ON DUPLICATE KEY UPDATE setting_value = setting_value;

-- ── 개발용 관리자 계정 (비밀번호: admin1234 → BCrypt 해시) ──
INSERT INTO admins (login_id, password_hash, name, role, is_active) VALUES
    ('admin', '$2a$10$dXJ3SW6G7P50lGmMQoeKhOelZJ2FG.VZL3ug0fE4ypyBHzDhFOKSS', '시스템관리자', 'SUPER_ADMIN', 1)
ON DUPLICATE KEY UPDATE name = name;

-- ── 개발용 강사 ──
INSERT INTO instructors (public_id, name, phone, status) VALUES
    ('dev_instructor_001', '박지영', '010-1111-2222', 'ACTIVE'),
    ('dev_instructor_002', '이수진', '010-3333-4444', 'ACTIVE'),
    ('dev_instructor_003', '최재훈', '010-5555-6666', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = name;

-- ── 강사 근무 가능 시간 (정리 후 삽입) ──
DELETE FROM instructor_available_times WHERE instructor_id IN
    (SELECT id FROM instructors WHERE public_id IN ('dev_instructor_001','dev_instructor_002','dev_instructor_003'));

INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'MONDAY', '09:00:00', '18:00:00' FROM instructors WHERE public_id = 'dev_instructor_001';
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'TUESDAY', '09:00:00', '18:00:00' FROM instructors WHERE public_id = 'dev_instructor_001';
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'WEDNESDAY', '09:00:00', '18:00:00' FROM instructors WHERE public_id = 'dev_instructor_001';
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'THURSDAY', '09:00:00', '18:00:00' FROM instructors WHERE public_id = 'dev_instructor_001';
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'FRIDAY', '09:00:00', '18:00:00' FROM instructors WHERE public_id = 'dev_instructor_001';

INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'MONDAY', '10:00:00', '19:00:00' FROM instructors WHERE public_id = 'dev_instructor_002';
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'WEDNESDAY', '10:00:00', '19:00:00' FROM instructors WHERE public_id = 'dev_instructor_002';
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'FRIDAY', '10:00:00', '19:00:00' FROM instructors WHERE public_id = 'dev_instructor_002';

INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'TUESDAY', '09:00:00', '17:00:00' FROM instructors WHERE public_id = 'dev_instructor_003';
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'THURSDAY', '09:00:00', '17:00:00' FROM instructors WHERE public_id = 'dev_instructor_003';
INSERT INTO instructor_available_times (instructor_id, day_of_week, start_time, end_time)
SELECT id, 'SATURDAY', '09:00:00', '17:00:00' FROM instructors WHERE public_id = 'dev_instructor_003';

-- ── 고정 스케줄 (정리 후 삽입) ──
DELETE FROM fixed_schedules WHERE instructor_id IN
    (SELECT id FROM instructors WHERE public_id IN ('dev_instructor_001','dev_instructor_002','dev_instructor_003'));

INSERT INTO fixed_schedules (instructor_id, lesson_type_id, day_of_week, start_time, end_time, is_active)
SELECT i.id, lt.id, 'MONDAY', '10:00:00', '10:50:00', 1
FROM instructors i, lesson_types lt WHERE i.public_id = 'dev_instructor_001' AND lt.name = '개인';

INSERT INTO fixed_schedules (instructor_id, lesson_type_id, day_of_week, start_time, end_time, is_active)
SELECT i.id, lt.id, 'MONDAY', '11:00:00', '11:50:00', 1
FROM instructors i, lesson_types lt WHERE i.public_id = 'dev_instructor_001' AND lt.name = '듀엣';

INSERT INTO fixed_schedules (instructor_id, lesson_type_id, day_of_week, start_time, end_time, is_active)
SELECT i.id, lt.id, 'WEDNESDAY', '14:00:00', '14:50:00', 1
FROM instructors i, lesson_types lt WHERE i.public_id = 'dev_instructor_001' AND lt.name = '그룹';

INSERT INTO fixed_schedules (instructor_id, lesson_type_id, day_of_week, start_time, end_time, is_active)
SELECT i.id, lt.id, 'WEDNESDAY', '10:00:00', '10:50:00', 1
FROM instructors i, lesson_types lt WHERE i.public_id = 'dev_instructor_002' AND lt.name = '개인';

INSERT INTO fixed_schedules (instructor_id, lesson_type_id, day_of_week, start_time, end_time, is_active)
SELECT i.id, lt.id, 'FRIDAY', '15:00:00', '15:50:00', 1
FROM instructors i, lesson_types lt WHERE i.public_id = 'dev_instructor_002' AND lt.name = '듀엣';

-- ── 공휴일 ──
INSERT INTO holidays (holiday_date, name) VALUES
    ('2026-08-15', '광복절'),
    ('2026-10-03', '개천절')
ON DUPLICATE KEY UPDATE name = name;

-- ── 정기권 종류 4종 ──
INSERT INTO membership_pass (public_id, name, price, total_count, validity_days, is_unlimited, monthly_limit, display_order) VALUES
    ('pass_8_group', '8회권', 180000, 8, 60, false, NULL, 1),
    ('pass_12_group', '12회권', 250000, 12, 90, false, NULL, 2),
    ('pass_unlimited', '무제한권', 350000, NULL, 30, true, 30, 3),
    ('pass_10_private', '개인 10회권', 500000, 10, 90, false, NULL, 4)
ON DUPLICATE KEY UPDATE name = name, price = price;

-- ── 정기권 종류-수업 유형 매핑 (정리 후 삽입) ──
DELETE FROM membership_pass_lesson_types WHERE membership_pass_id IN
    (SELECT id FROM membership_pass WHERE public_id IN ('pass_8_group','pass_12_group','pass_unlimited','pass_10_private'));

INSERT IGNORE INTO membership_pass_lesson_types (membership_pass_id, lesson_type_id)
SELECT mp.id, lt.id FROM membership_pass mp, lesson_types lt WHERE mp.public_id = 'pass_8_group' AND lt.name = '그룹';
INSERT IGNORE INTO membership_pass_lesson_types (membership_pass_id, lesson_type_id)
SELECT mp.id, lt.id FROM membership_pass mp, lesson_types lt WHERE mp.public_id = 'pass_8_group' AND lt.name = '듀엣';
INSERT IGNORE INTO membership_pass_lesson_types (membership_pass_id, lesson_type_id)
SELECT mp.id, lt.id FROM membership_pass mp, lesson_types lt WHERE mp.public_id = 'pass_12_group' AND lt.name = '그룹';
INSERT IGNORE INTO membership_pass_lesson_types (membership_pass_id, lesson_type_id)
SELECT mp.id, lt.id FROM membership_pass mp, lesson_types lt WHERE mp.public_id = 'pass_12_group' AND lt.name = '듀엣';
INSERT IGNORE INTO membership_pass_lesson_types (membership_pass_id, lesson_type_id)
SELECT mp.id, lt.id FROM membership_pass mp, lesson_types lt WHERE mp.public_id = 'pass_unlimited' AND lt.name = '그룹';
INSERT IGNORE INTO membership_pass_lesson_types (membership_pass_id, lesson_type_id)
SELECT mp.id, lt.id FROM membership_pass mp, lesson_types lt WHERE mp.public_id = 'pass_10_private' AND lt.name = '개인';
