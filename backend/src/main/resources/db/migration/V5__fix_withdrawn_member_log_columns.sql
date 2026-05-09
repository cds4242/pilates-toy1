-- V5: withdrawn_member_logs 컬럼 길이 수정
-- name_original, birth_encrypted_original은 암호화된 값을 저장하므로 512바이트 필요

ALTER TABLE withdrawn_member_logs MODIFY COLUMN name_original VARCHAR(512) NULL;
ALTER TABLE withdrawn_member_logs MODIFY COLUMN birth_encrypted_original VARCHAR(512) NULL;
