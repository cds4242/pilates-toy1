-- 필라테스 스튜디오 DB 초기화
-- Docker 최초 실행 시 자동 실행됨

-- 문자셋 확인
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 슬로우 쿼리 로그 활성화 (1초 이상)
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;

-- 기본 스키마는 docker-compose 환경변수로 생성됨 (pilates)
-- 테이블은 JPA ddl-auto=update로 로컬에서 자동 생성
