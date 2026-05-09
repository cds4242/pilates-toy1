-- V8: membership_pass 테이블에 audit 컬럼 추가 (BaseEntity 호환)

ALTER TABLE membership_pass ADD COLUMN created_by VARCHAR(50) NULL;
ALTER TABLE membership_pass ADD COLUMN updated_by VARCHAR(50) NULL;
