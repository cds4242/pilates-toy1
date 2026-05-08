-- =====================================================
-- V1: 기본 테이블 생성
-- 모든 테이블: ENGINE=InnoDB, CHARSET=utf8mb4, COLLATE=utf8mb4_unicode_ci
-- FK ON DELETE: RESTRICT (soft delete 사용하므로 물리 삭제 방지)
-- 예외: 종속 이력 테이블은 CASCADE
-- =====================================================

-- ── 강사 ──
CREATE TABLE instructors (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    public_id       VARCHAR(32)     NOT NULL,
    name            VARCHAR(50)     NOT NULL,
    phone           VARCHAR(20),
    status          VARCHAR(20)     NOT NULL,
    profile_image_url VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_instructors_public_id UNIQUE (public_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 강사 근무 가능 시간대 ──
CREATE TABLE instructor_available_times (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    instructor_id   BIGINT          NOT NULL,
    day_of_week     VARCHAR(10)     NOT NULL,
    start_time      TIME            NOT NULL,
    end_time        TIME            NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_instructor_avail_instructor
        FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 회원 ──
CREATE TABLE members (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    public_id       VARCHAR(32)     NOT NULL,
    name            VARCHAR(512)    NOT NULL COMMENT 'AES-256 암호화',
    phone_encrypted VARCHAR(512)    COMMENT 'AES-256 암호화, 탈퇴 시 NULL',
    phone_hash      VARCHAR(64)     COMMENT 'SHA-256 해시 (검색용), 탈퇴 시 NULL',
    birth_encrypted VARCHAR(512)    COMMENT 'AES-256 암호화',
    gender          VARCHAR(10)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    instructor_id   BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_members_public_id UNIQUE (public_id),
    CONSTRAINT uk_members_phone_hash UNIQUE (phone_hash),
    CONSTRAINT fk_members_instructor
        FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 탈퇴 회원 로그 ──
CREATE TABLE withdrawn_member_logs (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    member_id                   BIGINT          NOT NULL,
    phone_hash_original         VARCHAR(64)     COMMENT '탈퇴 전 phone_hash (30일 후 NULL)',
    name_original               VARCHAR(50)     COMMENT '탈퇴 전 이름 (30일 후 NULL)',
    birth_encrypted_original    VARCHAR(255)    COMMENT '탈퇴 전 암호화 생년월일 (30일 후 NULL)',
    withdrawn_at                TIMESTAMP       NOT NULL,
    withdrawal_reason           VARCHAR(255),
    anonymized                  TINYINT(1)      NOT NULL DEFAULT 0,
    anonymized_at               TIMESTAMP       NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_withdrawn_logs_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 회원 메모 ──
CREATE TABLE member_memos (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    member_id       BIGINT          NOT NULL,
    instructor_id   BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_member_memos_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_memos_instructor
        FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 수업 유형 마스터 ──
CREATE TABLE lesson_types (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL,
    max_capacity    INT             NOT NULL,
    duration_minutes INT            NOT NULL,
    deduction_count INT             NOT NULL DEFAULT 1 COMMENT '예약 시 정기권 차감 횟수',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 주간 고정 반복 스케줄 ──
CREATE TABLE fixed_schedules (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    instructor_id   BIGINT          NOT NULL,
    lesson_type_id  BIGINT          NOT NULL,
    day_of_week     VARCHAR(10)     NOT NULL,
    start_time      TIME            NOT NULL,
    end_time        TIME            NOT NULL,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_fixed_schedules_instructor
        FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE RESTRICT,
    CONSTRAINT fk_fixed_schedules_lesson_type
        FOREIGN KEY (lesson_type_id) REFERENCES lesson_types(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 수업 시간표 (실제 날짜별) ──
CREATE TABLE class_schedules (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    instructor_id   BIGINT          NOT NULL,
    lesson_type_id  BIGINT          NOT NULL,
    fixed_schedule_id BIGINT        COMMENT '반복 생성 원본 (단건이면 NULL)',
    class_date      DATE            NOT NULL,
    start_time      TIME            NOT NULL,
    end_time        TIME            NOT NULL,
    max_capacity    INT             NOT NULL,
    current_count   INT             NOT NULL DEFAULT 0,
    version         INT             NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_class_schedules_instructor
        FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE RESTRICT,
    CONSTRAINT fk_class_schedules_lesson_type
        FOREIGN KEY (lesson_type_id) REFERENCES lesson_types(id) ON DELETE RESTRICT,
    CONSTRAINT fk_class_schedules_fixed_schedule
        FOREIGN KEY (fixed_schedule_id) REFERENCES fixed_schedules(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 정기권 (수업 유형 매핑은 membership_lesson_types로 분리) ──
CREATE TABLE memberships (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    public_id       VARCHAR(32)     NOT NULL,
    member_id       BIGINT          NOT NULL,
    total_count     INT             NOT NULL,
    remaining_count INT             NOT NULL,
    is_unlimited    TINYINT(1)      NOT NULL DEFAULT 0,
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    price           DECIMAL(10,0)   NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_memberships_public_id UNIQUE (public_id),
    CONSTRAINT fk_memberships_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 정기권-수업 유형 매핑 (1:N) ──
CREATE TABLE membership_lesson_types (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    membership_id   BIGINT          NOT NULL,
    lesson_type_id  BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_mlt_membership_lesson_type UNIQUE (membership_id, lesson_type_id),
    CONSTRAINT fk_mlt_membership
        FOREIGN KEY (membership_id) REFERENCES memberships(id) ON DELETE CASCADE,
    CONSTRAINT fk_mlt_lesson_type
        FOREIGN KEY (lesson_type_id) REFERENCES lesson_types(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 정기권 홀딩 이력 ──
CREATE TABLE membership_holdings (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    membership_id   BIGINT          NOT NULL,
    hold_start_date DATE            NOT NULL,
    hold_end_date   DATE,
    reason          VARCHAR(500),
    extended_days   INT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_membership_holdings_membership
        FOREIGN KEY (membership_id) REFERENCES memberships(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 예약 ──
CREATE TABLE reservations (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    member_id       BIGINT          NOT NULL,
    class_schedule_id BIGINT        NOT NULL,
    membership_id   BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    wait_order      INT             COMMENT '대기 순번 (NULL이면 확정)',
    cancel_reason   VARCHAR(500),
    cancelled_at    TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP       NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservations_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_class_schedule
        FOREIGN KEY (class_schedule_id) REFERENCES class_schedules(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_membership
        FOREIGN KEY (membership_id) REFERENCES memberships(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 출석 ──
CREATE TABLE attendances (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    reservation_id  BIGINT          NOT NULL,
    member_id       BIGINT          NOT NULL,
    class_schedule_id BIGINT        NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    checked_at      TIMESTAMP       NULL,
    checked_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendances_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_attendances_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendances_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendances_class_schedule
        FOREIGN KEY (class_schedule_id) REFERENCES class_schedules(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 결제 ──
CREATE TABLE payments (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        VARCHAR(64)     NOT NULL COMMENT '결제 고유 번호 (중복 방지)',
    member_id       BIGINT          NOT NULL,
    membership_id   BIGINT          NOT NULL,
    amount          DECIMAL(10,0)   NOT NULL,
    method          VARCHAR(20)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    refund_amount   DECIMAL(10,0),
    refund_reason   VARCHAR(500),
    paid_at         TIMESTAMP       NULL,
    refunded_at     TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_order_id UNIQUE (order_id),
    CONSTRAINT fk_payments_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_membership
        FOREIGN KEY (membership_id) REFERENCES memberships(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 알림 ──
CREATE TABLE notifications (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    member_id       BIGINT          NOT NULL,
    type            VARCHAR(30)     NOT NULL,
    template_code   VARCHAR(50),
    content         TEXT,
    status          VARCHAR(20)     NOT NULL,
    failure_reason  VARCHAR(500),
    scheduled_at    TIMESTAMP       NULL,
    sent_at         TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 관리자 계정 ──
CREATE TABLE admins (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    login_id        VARCHAR(50)     NOT NULL,
    password_hash   VARCHAR(256)    NOT NULL COMMENT 'BCrypt 해시',
    name            VARCHAR(50)     NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    instructor_id   BIGINT          COMMENT '강사 역할인 경우 연결',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    last_login_at   TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admins_login_id UNIQUE (login_id),
    CONSTRAINT fk_admins_instructor
        FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 관리자 감사 로그 ──
CREATE TABLE admin_audit_logs (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    admin_id        BIGINT          NOT NULL,
    action          VARCHAR(30)     NOT NULL,
    target_type     VARCHAR(30)     NOT NULL,
    target_id       BIGINT,
    detail          TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 스튜디오 설정 ──
CREATE TABLE studio_settings (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    setting_key     VARCHAR(100)    NOT NULL,
    setting_value   VARCHAR(500),
    description     VARCHAR(500),
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_studio_settings_key UNIQUE (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 휴무일 ──
CREATE TABLE holidays (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    holiday_date    DATE            NOT NULL,
    name            VARCHAR(100),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_holidays_date UNIQUE (holiday_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
