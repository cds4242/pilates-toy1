-- V14: 수강권 상품 관리 확장 (노출 여부, 활성 상태, 판매 시작일, 카테고리)
ALTER TABLE membership_pass
    ADD COLUMN is_visible       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '회원 노출 여부 (1=노출, 0=숨김)',
    ADD COLUMN is_active        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '판매 활성 상태 (1=판매중, 0=판매중지)',
    ADD COLUMN sale_start_date  DATE         NULL COMMENT '판매 시작일 (NULL이면 즉시 판매)',
    ADD COLUMN sale_end_date    DATE         NULL COMMENT '판매 종료일 (NULL이면 무기한)',
    ADD COLUMN category         VARCHAR(20)  NULL COMMENT '카테고리 (PERSONAL/GROUP/UNLIMITED)',
    ADD COLUMN description      VARCHAR(500) NULL COMMENT '상품 설명';
