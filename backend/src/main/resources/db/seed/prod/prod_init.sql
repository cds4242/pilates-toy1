-- =====================================================
-- 운영 초기 세팅 (1회 수동 실행)
-- 실행 전: 환경변수 설정 필수 (아래 README.md 참고)
-- 실행 방법:
--   INITIAL_ADMIN_PASSWORD_HASH=$(echo -n '비밀번호' | python3 -c "import bcrypt,sys; print(bcrypt.hashpw(sys.stdin.read().encode(), bcrypt.gensalt(12)).decode())")
--   envsubst < prod_init.sql | mysql -u pilates -p pilates
-- =====================================================

-- ── 수업 유형 ──
INSERT INTO lesson_types (name, max_capacity, duration_minutes, deduction_count, is_active) VALUES
    ('개인', 1, 50, 2, 1),
    ('듀엣', 2, 50, 1, 1),
    ('그룹', 8, 50, 1, 1),
    ('체험', 1, 50, 0, 1);

-- ── 스튜디오 설정 ──
INSERT INTO studio_settings (setting_key, setting_value, description) VALUES
    ('CANCEL_DEADLINE_HOURS', '2', '무료 취소 가능 시간 (수업 시작 N시간 전)'),
    ('UNLIMITED_MONTHLY_LIMIT', '30', '무제한권 월 최대 이용 횟수'),
    ('DEFAULT_LESSON_DURATION', '50', '기본 수업 시간 (분)'),
    ('NO_SHOW_AUTO_MARK_MINUTES', '30', '수업 종료 후 N분 뒤 미출석 자동 노쇼 처리'),
    ('REMINDER_1DAY_HOUR', '20', '전날 리마인더 발송 시각 (시)'),
    ('REMINDER_SAME_DAY_HOURS', '2', '당일 리마인더 (수업 N시간 전)'),
    ('MEMBERSHIP_EXPIRY_ALERT_DAYS', '7,3', '정기권 만료 알림 (D-N일)'),
    ('MEMBERSHIP_LOW_COUNT_ALERT', '3', '정기권 잔여 횟수 부족 알림 기준');

-- ── 초기 슈퍼 관리자 ──
-- 환경변수 미설정 시 INSERT 실패 → 운영 배포 차단 효과
-- INITIAL_ADMIN_PASSWORD_HASH: BCrypt 해시 (평문 저장 금지)
INSERT INTO admins (login_id, password_hash, name, role, is_active, created_at, updated_at) VALUES
    ('${INITIAL_ADMIN_LOGIN_ID}', '${INITIAL_ADMIN_PASSWORD_HASH}', '초기관리자', 'SUPER_ADMIN', 1, NOW(), NOW());
