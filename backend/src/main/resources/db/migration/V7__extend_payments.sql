-- V7: payments 테이블 확장 (결제 도메인 STEP 7)

-- 토스 paymentKey (승인 후 저장)
ALTER TABLE payments ADD COLUMN payment_key VARCHAR(200) NULL AFTER order_id;

-- 정기권 종류 (어떤 상품을 구매했는지)
ALTER TABLE payments ADD COLUMN membership_pass_id BIGINT NULL AFTER member_id;
ALTER TABLE payments ADD CONSTRAINT fk_payments_membership_pass
    FOREIGN KEY (membership_pass_id) REFERENCES membership_pass(id);

-- membership_id를 NULL 허용으로 변경 (결제 생성 시점에 정기권 미발급)
ALTER TABLE payments MODIFY COLUMN membership_id BIGINT NULL;

-- 낙관적 락 (동시 환불 방지)
ALTER TABLE payments ADD COLUMN version INT NOT NULL DEFAULT 0;

-- 결제 상세 (카드사명 등)
ALTER TABLE payments ADD COLUMN payment_method_detail VARCHAR(100) NULL AFTER method;
