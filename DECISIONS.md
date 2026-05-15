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

## D-008: NAS 박제 동결 (2026-05-16)
**결정**: NAS 박제용 추가 작업 종료. 현 시점 NAS 배포본을 `nas-snapshot/`에 박제하고 그것으로만 복구 가능하게 유지.
**이유**:
- D-006으로 호스팅 전략이 Railway 풀스택으로 전환됨 → NAS 박제는 보조 자산
- 필라테스 메인 코드가 Railway용으로 진화하면 NAS 박제 모드(export + basePath)와 충돌 누적
- "박제는 동결, 메인은 진화" 분리가 두 노선 모두 안전
**범위**:
- 박제 대상: `/p1` (시연) + `/portfolio` (매뉴얼) + 루트 `index.html` + Apache `apache2.conf`
- 박제 위치: `D:/ai/toy1/nas-snapshot/nas-snapshot-pilates-20260516.tar.gz` (+ 압축 푼 사본)
- 복구 절차: `nas-snapshot/README.md` 참조
**금지 사항**:
- 박제 디렉토리를 더 이상 덮어쓰지 않음 (재빌드·재배포 X)
- 메인 코드가 박제 호환을 위해 양보하지 않음 (next.config.ts의 export 모드 잔재는 그대로 두되 손대지 않음)
**복구 트리거**: NAS의 /p1 또는 /portfolio가 깨졌을 때만. 그 외 상황에서는 박제본을 건드리지 않는다.

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

---

(앞으로 결정 사항 추가 시 D-XXX 번호 부여)
