# DECISIONS — 주요 결정 사항

> 코드만 봐서는 알 수 없는 "왜 이렇게 했는가" 기록.
> WORKLOG.md가 "무엇을"이면, 여기는 "왜".

---

## D-001: 정기권 차감 시점 (예약 vs 출석)
**결정**: 예약 시점 차감
**이유**:
- 명세 4.2에 "정기권 자동 복구" 취소 시점 명시 = 예약 시점 차감 전제
- 출석 시점 차감 시 "잔여 1회로 5개 예약" 악용 가능
- 단순성 = 안전성
**대안 검토**: 출석 시점 차감 (옵션 B) — 기각

## D-002: 강사 휴강 알림 (자동 vs 수동)
**결정**: 자동 알림 X, 관리자가 수동 처리
**이유**: 변경 합의서 #1 (의뢰인 정책)

## D-003: 운영 admin 시드 방식
**결정**: prod_init.sql + 환경변수 (INITIAL_ADMIN_PASSWORD)
**이유**:
- 평문 노출 방지
- 환경변수 미설정 시 INSERT 실패 = 운영 배포 차단 효과

## D-004: 시뮬레이션 페이스 vs 실전 페이스
**결정**: 시뮬레이션은 인계 문서 + 자산화 위주, 실제 배포 X
**이유**: 가상 시나리오에서 진짜 배포는 비용 대비 효과 낮음
**예외**: 포트폴리오 공개용으로 Phase A 3개만 처리 예정

## D-005: 자산 재활용 전략 (Rule of three)
**결정**: N=2 단계에서 추상화 X. 복사 후 수정 + COMMON_CANDIDATES.md 메모만.
**이유**: 1개 사례에서 추상화하면 필라테스 모양으로 굳어버림
**시점**: N=3 도메인(toy3)에서 academy-core 추출 검토

---

## D-006: 호스팅 전략 (NAS 박제 → 풀스택 SaaS)
**결정**: NAS 박제는 종료, Railway 풀스택 배포로 전환
**이유**:
- NAS 박제(https://dsjh.synology.me:8443/p1)는 정적 export만 = "구경" 수준
- 본인 목표는 "실제 호스팅 경험" = 백엔드+DB 포함 풀스택
- 비표준 포트(8443) + 자체 도메인 = 의뢰인 시연 신뢰도 낮음
**대안 검토**:
- Vercel: Next.js 친화, 하지만 백엔드 별도 → 통합 경험 X (기각)
- Render: 무료 더 김, MySQL 미지원 → PG 마이그레이션 부담 (기각)
- Fly.io: 256MB RAM 빡빡 → Spring Boot 부담 (기각)
- AWS: 진짜 클라우드지만 셋업 복잡 → 의뢰인 비용으로 다음에 (기각)
- **Railway 채택**: MySQL 그대로, $5 크레딧 1~2개월 운영, 셋업 단순
**비용 관점**: 시뮬레이션은 $5 크레딧 내 무료. 실 외주 시 의뢰인 비용 청구.

## D-007: Phase A 우선순위 재조정
**결정**: 풀스택 배포 경험을 1단계로 격상, 단독 프론트 호스팅(#3) 스킵
**이유**: NAS 박제로 정적 호스팅 경험 완료, 다음 단계는 풀스택
**순서**: 시드 → 로컬 QA → Docker 검증 → Railway 배포

## D-008: NAS 박제 의도적 동결 (2026-05-16)
**결정**: NAS 박제(https://dsjh.synology.me:8443/p1)를 **의도적으로 동결**하여 시연용 영구 보존. 폐기 아님. Railway 풀스택 배포는 별도 트랙으로 병행 진행.
**이유**:
- NAS 박제는 정적 시연 자산으로서 **고유 가치 유지** — 의뢰인·포트폴리오 시연용 영구 슬롯 (URL 안정성)
- Railway 풀스택은 "다음 단계"이지 NAS의 대체재가 아님 — 두 트랙은 서로 다른 목적
  - NAS 박제: 정적 시연, 무한 운영비 0, URL 영속
  - Railway: 동적 풀스택, 백엔드+DB, 실 호스팅 경험
- 메인 코드가 Railway용으로 진화하면 NAS 박제 모드(export + basePath)와 충돌 누적 → 분리 보존 필수
- "박제는 동결, 메인은 진화" 분리가 두 노선 모두 안전
**범위**:
- 박제 대상: `/p1` (시연) + `/portfolio` (매뉴얼) + 루트 `index.html` + Apache `apache2.conf`
- 박제 위치: `D:/ai/toy1/nas-snapshot/nas-snapshot-pilates-20260516.tar.gz` (+ 압축 푼 사본)
- 복구 절차: `nas-snapshot/README.md` 참조
**금지 사항**:
- 박제 디렉토리를 더 이상 덮어쓰지 않음 (재빌드·재배포 X)
- 메인 코드가 박제 호환을 위해 양보하지 않음 (next.config.ts의 export 모드 잔재는 D-009로 standalone 전환 — 박제본은 이미 분리 보존됨)
- NAS 박제를 "구버전·곧 폐기" 취급 금지 — 의도적·영구 동결 자산
**복구 트리거**: NAS의 /p1 또는 /portfolio가 깨졌을 때만. 그 외 상황에서는 박제본을 건드리지 않는다.
**상태**: 영구 활성 시연 슬롯 (Railway 가동 후에도 병행 운영)

## D-009: Next.js 빌드 모드 — Railway용 standalone 전환 (옵션 A 채택)
**결정**: `frontend/next.config.ts`의 `output: "export"` + `basePath: "/p1"` 잔재를 standalone으로 전환. NAS 박제 모드는 폐기.
**이유**:
- Docker 검수에서 frontend/Dockerfile(standalone 가정)과 next.config.ts(export 모드)가 정면 충돌 발견 (동기화 누락 사례 3호)
- D-008로 NAS 배포본은 이미 박제되어 보존됨 → 소스 박제 모드를 유지할 필요 없음
- Railway 풀스택 배포가 메인 노선 (D-006)
**대안 검토**:
- B: 환경변수 분기 (DEPLOY_TARGET=nas/railway) → 코드 복잡도 ↑, 박제 추가 작업 없다는 D-008과 모순 → 기각
- C: export 유지 + nginx 정적 서빙으로 Railway 배포 → 동적 라우팅 불가, 백엔드 연결 패턴 변경 → 기각
**연계 작업**: docker_fix_prompt.md 실행 시 함께 처리

## D-010: NEXT_PUBLIC_* 환경변수 빌드 시점 주입
**결정**: `NEXT_PUBLIC_API_URL` 등 클라이언트 노출 환경변수는 docker-compose의 `build.args` + Dockerfile의 `ARG`로 빌드 시점에 주입. 런타임 `environment:` 사용 안 함.
**이유**:
- Next.js의 `NEXT_PUBLIC_*`은 빌드 시점에 JS 번들에 박힘 → 런타임 environment는 효과 없음
- 현재 docker-compose.prod.yml:80은 런타임 주입이라 브라우저가 backend URL 못 찾음
- Railway 배포 시에도 동일 함정 — 빌드 시점에 frontend 서비스가 backend public URL을 알아야 함
**연계 작업**: docker_fix_prompt.md 실행 시 함께 처리

## D-011: 로깅 전략 — stdout 단일화 (12-Factor App §XI)
**결정**: 모든 환경(local/prod/default)에서 로그를 stdout으로만 출력. 파일 기반 RollingFileAppender(`./logs/application.log`, `request.log`, `error.log`) 전부 제거.
**이유**:
- 12-Factor App §XI Logs: 앱은 로그를 event stream으로 stdout에 쓰고, 수집/회전/저장은 실행 환경(Docker/Railway/CloudWatch)에 위임한다
- 컨테이너는 일회용 — 컨테이너 내부 파일 로그는 컨테이너 소멸 시 손실
- Railway 무료 티어/Heroku/Cloud Run 등 PaaS는 파일 로그 안 보고 stdout만 수집
- 직전 docker compose 검증에서 `/app/logs/` 디렉토리 부재로 backend Restarting 루프 발생 — 환경 의존적 사이드이펙트
- prod 프로파일은 `LogstashEncoder`로 JSON 구조화 출력 → docker logs / Railway 대시보드 / 외부 수집기와 자연 연동
**대안 검토**:
- 옵션 A (Dockerfile에 `mkdir /app/logs + chown`): 로컬은 되지만 Railway 등 PaaS에서 무의미, 환경별 분기 발생 — 기각
- 옵션 B (docker-compose named volume): 로컬 한정 해결, Railway 환경과 격차 — 기각
**트레이드오프**:
- 로그 회전 자동 관리 불가 → Docker/Railway 위임 (docker logs 옵션 `--max-size`)
- 파일 grep 불가 → `docker compose logs backend | grep`로 우회
- 영속성은 실행 환경 의존 (Railway 무료 티어 보관 기간 단기)
**시뮬레이션 의도와 일치**: 운영 환경과 동일한 12-Factor 패턴 익히기

## D-012: 시뮬레이션·Railway용 demo 프로파일 분리
**결정**: `prod`와 별개로 `demo` 프로파일 신규. Mock 통합 + MySQL + 시드 데이터 조합.
**배경**:
- Step 2 로컬 docker compose 검증 중 `SPRING_PROFILES_ACTIVE=prod`로 부팅 시 `No qualifying bean of type 'SmsService'`로 12회 재시작 (2026-05-16 발견)
- 원인: `MockSmsService`/`MockTossPaymentClient`는 `@Profile({"local","local-h2","test","portfolio"})` 한정 — prod에 구현체 없음
- prod = 실 NHN Toast/Toss 키 필요한 실 운영용. 시뮬레이션·Railway 시연은 Mock으로 충분
- "docker compose에서 prod 프로파일 사용" 자체가 의도 불일치 — 본인 동기화 누락 사례 4호
**새 프로파일 구조**:
| 프로파일 | DB | SMS/카카오/토스 | 시드 | 용도 |
|---|---|---|---|---|
| `local` | MySQL (Docker) | Mock | ✅ | 개발자 PC |
| `local-h2` | H2 | Mock | ✅ | DB 없이 단독 실행 |
| `test` | H2 | Mock | — | 자동화 테스트 |
| `portfolio` | H2 + 임베디드 Redis | Mock | ✅ | NAS 박제 시연 (단독 JAR) |
| `demo` | MySQL (외부) + Redis (외부) | Mock | ✅ | **Railway 풀스택 시연 (NEW)** |
| `prod` | MySQL + Redis | 실 통합 (실 키 필요) | — | 실 운영 |
**시뮬레이션 의도와 일치**: 포트폴리오 풀스택 시연 시 실 SMS 발송·실 결제 X
**구현 변경**:
- `application-demo.yml` 신규
- `MockSmsService`/`MockKakaoAlimtalkClient`/`MockTossPaymentClient`/`DemoSeedRunner` `@Profile`에 `demo` 추가

## D-013: docker-compose 파일명 컨벤션
**결정**: `docker-compose.prod.yml` 파일명 유지, 내부 `SPRING_PROFILES_ACTIVE`만 demo로 운용.
**이유**:
- 파일명 `prod` = production-grade 구동 방식 (Docker Compose 업계 컨벤션)
- Spring `prod` 프로파일과 같은 단어지만 다른 개념
- 6개 문서(WORKLOG, REUSABLE_NOTES, PROJECT_STRUCTURE, STEP14, etc) 참조 동기화 회피
- 실 운영 진입 시점에 환경변수만 `prod`로 바꾸면 됨 (파일 변경 X)
**명명 충돌 완화책**: docker-compose.prod.yml 상단에 명시적 주석 추가 + 본 D-013 참조
**대안 검토**:
- 옵션 A (`docker-compose.demo.yml`로 리네임): 6개 문서 동기화 필요 — 기각
- 옵션 C (.prod.yml + .demo.yml 분리): YAGNI, 진짜 prod 운영 시점에 분리 — 기각

## D-014: Railway Trial Workspace 채택 (무료 크레딧 + 카드 미등록)
**결정**: Railway 무료 Trial 사용, 신용카드 등록 X.
**이유**:
- 본인 목적 = 배포 경험 + 납품 연습 (24시간 운영 X)
- $5 크레딧으로 4~6주 운영 가능 (idle 기준)
- 카드 미등록 = 한도 초과 시 자동 정지 (안전망)
**대안 검토**:
- Render (MySQL 미지원, PG 마이그레이션 부담) (기각)
- Fly.io (셋업 복잡, 256MB RAM 빡빡) (기각)
- Vercel (백엔드 별도 배포 필요) (기각)
- AWS Free Tier (1년 후 유료, 셋업 복잡) (기각)
**비용 관리 정책**:
- 시연 후 Pause 또는 Delete (본인 결정)
- 진짜 외주 시 의뢰인 비용으로 Hobby Plan 전환

---

(앞으로 결정 사항 추가 시 D-XXX 번호 부여)
