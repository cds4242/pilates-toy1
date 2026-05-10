-- V13: 강사 프로필 확장 (주소, 이메일, 자격증, 생년월일, 전문분야, 메모)
ALTER TABLE instructors
    ADD COLUMN email          VARCHAR(100)  NULL COMMENT '이메일',
    ADD COLUMN address        VARCHAR(500)  NULL COMMENT '주소',
    ADD COLUMN birth_date     DATE          NULL COMMENT '생년월일',
    ADD COLUMN specialty      VARCHAR(200)  NULL COMMENT '전문 분야 (콤마 구분)',
    ADD COLUMN certification  VARCHAR(500)  NULL COMMENT '자격증 (콤마 구분)',
    ADD COLUMN working_days   VARCHAR(100)  NULL COMMENT '근무 요일 (MON,TUE,WED 등)',
    ADD COLUMN memo           TEXT          NULL COMMENT '관리자 메모';
