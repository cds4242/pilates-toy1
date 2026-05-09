-- V3: 회원 프로필 사진 컬럼 추가
-- 변경 합의서 #1 (2026-05-12 적용)

ALTER TABLE members
    ADD COLUMN profile_image_url VARCHAR(500) NULL COMMENT '프로필 사진 URL (Cloudflare R2)' AFTER status,
    ADD COLUMN profile_image_uploaded_at TIMESTAMP NULL COMMENT '프로필 사진 업로드 시각' AFTER profile_image_url;

-- 인덱스 불필요: profile_image_url은 조회 조건으로 사용하지 않음
