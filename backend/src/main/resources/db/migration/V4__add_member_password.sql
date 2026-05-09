-- V4: 회원 비밀번호 컬럼 추가
-- Phase 3: 회원가입/로그인 기능

ALTER TABLE members ADD COLUMN password_hash VARCHAR(255) NULL;
