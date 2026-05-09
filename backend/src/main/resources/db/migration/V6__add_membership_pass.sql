-- V6: 정기권 종류(상품 카탈로그) 테이블 신설 (STEP 3 누락 보강)

-- 정기권 종류 테이블
CREATE TABLE membership_pass (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    public_id       VARCHAR(32)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    price           DECIMAL(10, 0)  NOT NULL,
    total_count     INT             NULL COMMENT '무제한권은 NULL',
    validity_days   INT             NOT NULL,
    is_unlimited    TINYINT(1)      NOT NULL DEFAULT 0,
    monthly_limit   INT             NULL COMMENT '무제한권만 사용',
    display_order   INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_membership_pass_public_id UNIQUE (public_id),
    INDEX idx_membership_pass_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 정기권 종류 ↔ 수업 유형 매핑
CREATE TABLE membership_pass_lesson_types (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    membership_pass_id  BIGINT      NOT NULL,
    lesson_type_id      BIGINT      NOT NULL,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_mplt UNIQUE (membership_pass_id, lesson_type_id),
    CONSTRAINT fk_mplt_pass FOREIGN KEY (membership_pass_id) REFERENCES membership_pass(id) ON DELETE CASCADE,
    CONSTRAINT fk_mplt_lesson_type FOREIGN KEY (lesson_type_id) REFERENCES lesson_types(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- memberships 테이블에 membership_pass_id FK 추가
ALTER TABLE memberships ADD COLUMN membership_pass_id BIGINT NULL AFTER member_id;
ALTER TABLE memberships ADD INDEX idx_memberships_pass_id (membership_pass_id);
ALTER TABLE memberships ADD CONSTRAINT fk_memberships_pass FOREIGN KEY (membership_pass_id) REFERENCES membership_pass(id);

-- TODO: 운영 데이터 정합성 검증 후 membership_pass_id를 NOT NULL로 변경
