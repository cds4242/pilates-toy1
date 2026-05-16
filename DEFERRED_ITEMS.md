# DEFERRED ITEMS — 보류 항목

> 알면서 미루는 작업 목록. WORKLOG는 "한 것", DECISIONS는 "왜", 여기는 "나중에".
> 우선순위 별표: ★★★ 즉시 / ★★ 다음 마일스톤 / ★ 한가할 때

---

## NAS 박제 잔재 정리 (★, 30분)

**대상**:
- `frontend/snapshot-rewrite.mjs`, `snapshot-pages.mjs` 등 NAS 박제 전용 후처리 스크립트
- `NEXT_PUBLIC_BASE_PATH` 참조 코드 3곳:
  - `frontend/src/app/admin-login/page.tsx:93` (`studio3.jpg` src)
  - `frontend/src/app/instructor-login/page.tsx:94` (`studio2.jpg` src)
  - `frontend/src/app/(auth)/login/page.tsx:193` (`studio.jpg` src)

**현재 상태**:
- D-008(NAS 박제 의도적 동결) + D-009(Next.js standalone 전환) 적용 후 `NEXT_PUBLIC_BASE_PATH` 환경변수 미정의 상태
- 코드는 `process.env.NEXT_PUBLIC_BASE_PATH || ""` fallback이라 정상 동작 (`""+"/studio.jpg"` = `/studio.jpg`)
- frontend Docker 빌드 22페이지 모두 정상 prerender됨이 증거

**보존도 가능한 이유**:
- D-008로 NAS 박제는 "의도적·영구 동결" — 재생성 안 함이 원칙이지만, 박제본 복구 트리거 발동 시 스크립트 재사용 가능성 있음
- 죽은 참조 정리는 코드 위생 차원이며 동작에 영향 X

**처리 시점**: Railway 배포 완료 후 (Step 2 진입에 영향 없음)

**처리 방향 후보**:
- A. 삭제 — 박제는 동결, nas-snapshot/ 박제본이 진실의 원천 (D-008)
- B. 보존 + 주석 — "NAS 박제 재생성 전용, 평소 무시" 명시
- C. nas-snapshot/scripts/ 로 이전 — 박제본과 함께 보존
