# 트러블슈팅 가이드

---

## 1. 의뢰인 자주 만나는 문제

### "회원가입 인증번호 안 와요"

**확인 순서:**
1. 휴대폰 번호 형식 확인 (01012345678, 하이픈 없이)
2. 1분 내 재발송 불가 (SMS_001 에러)
3. 일 5회 발송 제한 (SMS_002 에러)
4. SMS 서비스 장애 시 → 개발자에게 SMS API 키 확인 요청

### "결제 실패해요"

**확인 순서:**
1. 토스페이먼츠 가맹점 상태 확인 (토스 관리자 페이지)
2. 결제 금액과 정기권 금액 일치 여부
3. 이미 처리된 결제인지 (PAY_002 중복 결제)
4. 네트워크 오류 시 → 잠시 후 재시도

### "예약 안 돼요"

**확인 순서:**
1. 사용 가능한 정기권이 있는지 (잔여 횟수 > 0, 유효기간 내)
2. 정원이 가득 찼는지 (RES_004)
3. 같은 시간에 다른 예약이 있는지 (RES_010 시간 겹침)
4. 이미 같은 수업에 예약했는지 (RES_002 중복)
5. 취소 후 재예약인지 → 정상 가능

### "출석 체크가 안 돼요"

**확인 순서:**
1. 수업 시작 시각 ~ 종료 30분 후 사이인지 (ATT_002)
2. 이미 출석 체크된 상태인지 (ATT_003)
3. 본인 담당 수업인지 (다른 강사 수업은 403)

---

## 2. 개발자용 (운영자가 개발자에게 전달)

### 백엔드 안 뜸

```bash
# 1. DB 연결 확인
docker ps  # pilates-mysql, pilates-redis 실행 중인지

# 2. 환경변수 확인
echo $ENCRYPTION_KEY  # AES-256 키 설정 필요
echo $JWT_SECRET      # JWT 서명 키 설정 필요

# 3. Flyway 마이그레이션 확인
cd backend
./gradlew bootRun --args='--spring.profiles.active=local' 2>&1 | grep "migration"

# 4. 포트 충돌
netstat -aon | grep :8080
```

### 프론트엔드 안 뜸

```bash
# 1. 의존성 설치
cd frontend
npm install

# 2. 환경변수
cat .env.local  # NEXT_PUBLIC_API_URL=http://localhost:8080

# 3. 백엔드 CORS 확인
curl -I -X OPTIONS http://localhost:8080/api/health \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"
# Access-Control-Allow-Origin 헤더 확인
```

### 알림 안 가요

1. **카카오 알림톡**: `ALIMTALK_SENDER_KEY`, `ALIMTALK_PFID` 환경변수 확인
2. **SMS**: MockSmsService 사용 중이면 실제 발송 안 됨 (로그만)
3. `app.alimtalk.provider` 설정 확인: `mock` → 테스트용, `nhn-toast` → 운영용

### DB 마이그레이션 실패

```bash
# Flyway 상태 확인
cd backend
./gradlew flywayInfo --args='--spring.profiles.active=local'

# 수동 복구 (주의: 데이터 손실 가능)
# flyway_schema_history 테이블에서 실패 레코드 삭제 후 재시도
```

---

## 3. 긴급 대응

### DB 백업·복구

```bash
# 백업
docker exec pilates-mysql mysqldump -u pilates -ppilates1234 pilates > backup_$(date +%Y%m%d).sql

# 복구
docker exec -i pilates-mysql mysql -u pilates -ppilates1234 pilates < backup_20260101.sql
```

### JWT 토큰 키 갱신

1. 새 시크릿 생성: `openssl rand -base64 32`
2. 환경변수 `JWT_SECRET` 업데이트
3. 백엔드 재시작 → 기존 모든 토큰 무효화 (사용자 재로그인 필요)

### AES 암호화 키 갱신

> **주의: 키 변경 시 기존 암호화 데이터 복호화 불가**

1. 기존 키로 전체 데이터 복호화 → 평문 백업
2. 새 키 설정 (`ENCRYPTION_KEY` 환경변수)
3. `ENCRYPTION_KEY_VERSION` 증가
4. 백엔드 재시작 → 점진적 재암호화

### 슈퍼 관리자 비밀번호 분실

```bash
# 1. BCrypt 해시 생성 (Java)
# BcryptGenTest.java 참조하여 새 해시 생성

# 2. DB 직접 업데이트
docker exec pilates-mysql mysql -u pilates -ppilates1234 pilates \
  -e "UPDATE admins SET password_hash='새_해시' WHERE login_id='admin'"

# 3. 백엔드 재시작 불필요 (DB 직접 반영)
```

---

## 4. 에러 코드 참조

| 코드 | 의미 | 대응 |
|------|------|------|
| COMMON_001 | 서버 내부 오류 | 개발자에게 로그 전달 |
| AUTH_001 | 토큰 만료 | 재로그인 |
| MEMBER_001 | 중복 가입 | 기존 계정으로 로그인 |
| MSHIP_007 | 잔여 횟수 부족 | 정기권 추가 구매 |
| RES_004 | 정원 초과 | 다른 시간대 예약 |
| PAY_004 | 결제 승인 실패 | 토스페이먼츠 상태 확인 |
| ADMIN_011 | 엑셀 형식 오류 | 템플릿 다시 다운로드 |
