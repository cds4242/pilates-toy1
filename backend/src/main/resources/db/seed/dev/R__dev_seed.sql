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
    ('admin', '$2a$12$pMjw7nM3uimwmexOxQ8WmuS4jUQvOtPswedJ.9fPkssqnDJv/hLXu', '시스템관리자', 'SUPER_ADMIN', 1)
ON DUPLICATE KEY UPDATE name = name;

-- ── 개발용 강사 admin 계정 (비밀번호: admin1234, 강사 로그인에서 사용) ──
-- 참고: instructor_id는 강사 삽입 후 서브쿼리로 연결
INSERT INTO admins (login_id, password_hash, name, role, is_active)
SELECT 'instructor1', '$2a$12$pMjw7nM3uimwmexOxQ8WmuS4jUQvOtPswedJ.9fPkssqnDJv/hLXu', '박지영', 'INSTRUCTOR', 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM admins WHERE login_id = 'instructor1');
UPDATE admins SET instructor_id = (SELECT id FROM instructors WHERE public_id = 'dev_instructor_001') WHERE login_id = 'instructor1' AND instructor_id IS NULL;

INSERT INTO admins (login_id, password_hash, name, role, is_active)
SELECT 'instructor2', '$2a$12$pMjw7nM3uimwmexOxQ8WmuS4jUQvOtPswedJ.9fPkssqnDJv/hLXu', '이수진', 'INSTRUCTOR', 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM admins WHERE login_id = 'instructor2');
UPDATE admins SET instructor_id = (SELECT id FROM instructors WHERE public_id = 'dev_instructor_002') WHERE login_id = 'instructor2' AND instructor_id IS NULL;

-- ── 개발용 강사 ──
INSERT INTO instructors (public_id, name, phone, status) VALUES
    ('dev_instructor_001', '박지영', '010-1111-2222', 'ACTIVE'),
    ('dev_instructor_002', '이수진', '010-3333-4444', 'ACTIVE'),
    ('dev_instructor_003', '최재훈', '010-5555-6666', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = name;

-- ── 강사 프로필 확장 정보 ──
UPDATE instructors SET
    specialty = '기구 필라테스, 매트 필라테스',
    certification = '필라테스 지도자 1급, 운동처방사',
    working_days = 'MON,TUE,WED,THU,FRI',
    email = 'jiyoung@studio.com',
    address = '서울시 강남구 테헤란로 123',
    birth_date = '1992-03-15',
    memo = '주력 강사. 그룹/개인 수업 모두 가능.'
WHERE public_id = 'dev_instructor_001';

UPDATE instructors SET
    specialty = '재활 필라테스, 산전산후',
    certification = '필라테스 지도자 2급, 물리치료사',
    working_days = 'MON,WED,FRI',
    email = 'soojin@studio.com',
    address = '서울시 서초구 서초대로 456',
    birth_date = '1995-07-22',
    memo = '재활 전문. 월/수/금 근무.'
WHERE public_id = 'dev_instructor_002';

UPDATE instructors SET
    specialty = '기구 필라테스, 체형교정',
    certification = '필라테스 지도자 1급',
    working_days = 'TUE,THU,SAT',
    email = 'jaehoon@studio.com',
    address = '서울시 송파구 올림픽로 789',
    birth_date = '1990-11-08',
    memo = '화/목/토 근무. 체형교정 전문.'
WHERE public_id = 'dev_instructor_003';

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
INSERT INTO membership_pass (public_id, name, price, total_count, validity_days, is_unlimited, monthly_limit, display_order, is_visible, is_active, category, description) VALUES
    ('pass_8_group', '8회권', 180000, 8, 60, false, NULL, 1, 1, 1, 'GROUP', '주 2회 추천, 그룹/듀엣 수업 가능'),
    ('pass_12_group', '12회권', 250000, 12, 90, false, NULL, 2, 1, 1, 'GROUP', '주 3회 추천, 그룹/듀엣 수업 가능'),
    ('pass_unlimited', '무제한권', 350000, NULL, 30, true, 30, 3, 1, 1, 'UNLIMITED', '한 달 무제한 수강, 월 30회까지'),
    ('pass_10_private', '개인 10회권', 500000, 10, 90, false, NULL, 4, 1, 1, 'PERSONAL', '1:1 개인 레슨 전용')
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

-- ── 알림 템플릿 5종 ──
INSERT INTO notification_templates (code, title, body, channel, is_active) VALUES
    ('RESERVATION_CONFIRM', '예약 확인', '[필라테스 OO점] {memberName}님, {date} {time} {className} 예약이 완료되었습니다.', 'ALIMTALK', TRUE),
    ('RESERVATION_CANCEL', '예약 취소', '[필라테스 OO점] {memberName}님, {date} {time} 예약이 취소되었습니다.', 'ALIMTALK', TRUE),
    ('REMINDER_1HOUR', '수업 1시간 전 리마인드', '[필라테스 OO점] {memberName}님, {time} {className} 수업이 1시간 후 시작됩니다.', 'ALIMTALK', TRUE),
    ('NEW_RESERVATION', '강사 새 예약 알림', '[필라테스 OO점] {instructorName} 강사님, {memberName}님이 {date} {time} 수업을 예약했습니다.', 'ALIMTALK', TRUE),
    ('MEMBERSHIP_EXPIRING', '정기권 만료 알림', '[필라테스 OO점] {memberName}님, 정기권이 {days}일 후 만료됩니다.', 'ALIMTALK', TRUE)
ON DUPLICATE KEY UPDATE title = title;
