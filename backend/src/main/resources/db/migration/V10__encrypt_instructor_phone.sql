-- =====================================================
-- V10: 강사 phone 암호화 (STEP 5 보강)
-- phone 평문 → phone_encrypted (AES) + phone_hash (SHA-256)
-- =====================================================

ALTER TABLE instructors
    ADD COLUMN phone_encrypted VARCHAR(500) NULL AFTER name,
    ADD COLUMN phone_hash VARCHAR(64) NULL AFTER phone_encrypted;

CREATE INDEX idx_instructors_phone_hash ON instructors(phone_hash);

-- 기존 phone 컬럼을 NULL 허용으로 변경 (점진 마이그레이션)
-- 운영 환경에서는 별도 마이그레이션 스크립트로 phone → phone_encrypted/phone_hash 변환 후 phone 컬럼 제거
ALTER TABLE instructors MODIFY COLUMN phone VARCHAR(20) NULL;
