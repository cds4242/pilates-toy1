-- V3: 회원 프로필 사진 컬럼 추가
-- 변경 합의서 #1 (2026-05-12 적용)

ALTER TABLE members ADD COLUMN profile_image_url VARCHAR(500) NULL;
ALTER TABLE members ADD COLUMN profile_image_uploaded_at TIMESTAMP NULL;

-- 인덱스 불필요: profile_image_url은 조회 조건으로 사용하지 않음
