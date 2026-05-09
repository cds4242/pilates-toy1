-- V3: 회원 프로필 사진 컬럼 추가
-- 변경 합의서 #1 (2026-05-12 적용)
-- 컬럼이 이미 존재하면 SELECT 1로 no-op

ALTER TABLE members ADD COLUMN profile_image_url VARCHAR(500) NULL;
ALTER TABLE members ADD COLUMN profile_image_uploaded_at TIMESTAMP NULL;
