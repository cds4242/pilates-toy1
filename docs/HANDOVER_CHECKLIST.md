# 인계 체크리스트

---

## 1. 환경 셋업 확인

- [ ] Java 21 (Eclipse Temurin 또는 GraalVM)
- [ ] Node.js 20+
- [ ] Docker + Docker Compose
- [ ] MySQL 8.0
- [ ] Redis 7
- [ ] Git

## 2. 환경변수 설정

- [ ] `.env` 파일 작성 (`.env.example` 참조)
- [ ] 운영 시크릿 별도 저장 (1Password, Vault 등)
- [ ] `JWT_SECRET` 생성: `openssl rand -base64 32`
- [ ] `ENCRYPTION_KEY` 생성: `openssl rand -base64 32`
- [ ] `INITIAL_ADMIN_PASSWORD_HASH` BCrypt 생성

## 3. 데이터 마이그레이션

- [ ] MySQL DB 생성 (`pilates` 스키마)
- [ ] Flyway 마이그레이션 실행 (V1~V12 자동 적용)
- [ ] super admin 계정 확인 (시드 또는 수동)
- [ ] 학원 설정 초기 입력 (`studio_settings` 8개 키)
- [ ] 수업 유형 등록 (개인/듀엣/그룹/체험)
- [ ] 강사 등록
- [ ] 정기권 종류 등록
- [ ] 고정 스케줄 설정 → 수업 자동 생성

## 4. 외부 서비스 연동

- [ ] 토스페이먼츠 가맹점 등록 + 시크릿 키 발급
- [ ] 토스 웹훅 URL 등록 (`/api/webhooks/toss`)
- [ ] 카카오 비즈메시지 (NHN Toast) 가입 + Sender Key
- [ ] 카카오 알림톡 템플릿 5종 등록 + 검수 통과
- [ ] SMS 게이트웨이 가입 + API Key
- [ ] 도메인 구입 + DNS A 레코드 설정
- [ ] HTTPS 인증서 (Let's Encrypt certbot 또는 AWS ACM)

## 5. 보안 점검

- [ ] `/api/admin/**` ROLE 검증 적용 확인 (SecurityConfig)
- [ ] CORS `allowed-origins`에 운영 도메인만 등록
- [ ] HTTPS 강제 (리버스 프록시 또는 Spring `server.ssl`)
- [ ] 환경변수로 시크릿 주입 (코드 하드코딩 없음)
- [ ] DB 비밀번호 변경 (기본값 사용 금지)
- [ ] Redis 비밀번호 설정
- [ ] H2 콘솔 비활성화 (운영)

## 6. 모니터링

- [ ] 로그 수집 (CloudWatch / ELK / Loki)
- [ ] 에러 알림 (Slack Webhook 또는 이메일)
- [ ] DB 백업 자동화 (일 1회 mysqldump 또는 RDS 스냅샷)
- [ ] 디스크 사용량 모니터링
- [ ] Actuator 헬스 엔드포인트 (`/actuator/health`)

## 7. 운영 교육

- [ ] 의뢰인 첫 로그인 시연 (admin / 초기 비밀번호)
- [ ] 일상 운영 시연 (회원 등록 → 정기권 → 수업 → 예약 → 출석)
- [ ] 대시보드 + 통계 + 엑셀 다운로드 시연
- [ ] 엑셀 일괄 등록 시연
- [ ] 자주 묻는 질문 (`docs/TROUBLESHOOTING.md` 전달)
- [ ] 운영 매뉴얼 PDF 전달 (`docs/OPERATION_MANUAL.md`)

## 8. 사후 지원

- [ ] 1개월 무상 유지보수 범위 합의
- [ ] 버그 신고 채널 설정 (카카오톡 / GitHub Issues)
- [ ] 긴급 연락처 교환
- [ ] v2 기능 요청 절차 합의 (서면 견적)
- [ ] 소스 코드 저장소 접근 권한 이전
