# NAS 박제 스냅샷 (필라테스 프로젝트)

> NAS(`https://dsjh.synology.me:8443`)에 올라가 있는 시연/매뉴얼 사이트의 **배포 산출물 박제본**.
> 이 시점 이후 NAS 박제용 추가 작업은 하지 않는다 (D-008 결정).
> NAS 사이트에 문제 생겼을 때 이 스냅샷으로만 복구 가능하면 충분.

## 스냅샷 정보

- **생성일**: 2026-05-16
- **출처 NAS**: `cds4242@192.168.0.30:22311`
- **출처 경로**: `/opt/share/apache2/htdocs/{p1,portfolio,index.html}` + `/opt/etc/apache2/apache2.conf`
- **외부 URL**:
  - 시연: `https://dsjh.synology.me:8443/p1/`
  - 매뉴얼: `https://dsjh.synology.me:8443/portfolio/`
  - 랜딩(index): `https://dsjh.synology.me:8443/`

## 디렉토리 구조

```
nas-snapshot/
├── README.md                              ← 이 파일
├── nas-snapshot-pilates-20260516.tar.gz   ← 압축본 (배포 시 이거만 NAS로 전송)
└── nas-snapshot-pilates-20260516/         ← 압축 풀어둔 사본 (diff 검토용)
    ├── htdocs/
    │   ├── index.html                     ← 랜딩 페이지 (5개 프로젝트 카드)
    │   ├── p1/                            ← 필라테스 시연 (Next.js export + mock)
    │   └── portfolio/                     ← 필라테스 매뉴얼/포트폴리오
    └── apache-conf/
        ├── apache2.conf                   ← 현재 활성 Apache 설정 (/p1 export rewrite 포함)
        └── apache2.conf.bak.before-p1-export  ← export 전환 직전 설정 (참고용)
```

## 복구 절차 (NAS에 다시 올리기)

### A. 전체 복구 (시연 + 매뉴얼 + 설정)

```bash
# 1) tar 파일을 NAS로 전송
scp -O -P 22311 D:/ai/toy1/nas-snapshot/nas-snapshot-pilates-20260516.tar.gz \
    cds4242@192.168.0.30:/tmp/

# 2) NAS에서 풀어서 덮어쓰기
ssh -p 22311 cds4242@192.168.0.30
sudo bash -c '
  cd /tmp && tar xzf nas-snapshot-pilates-20260516.tar.gz
  STAGE=/tmp/nas-snapshot-pilates-20260516

  # 기존 백업 (안전망)
  TS=$(date +%Y%m%d-%H%M%S)
  mv /opt/share/apache2/htdocs/p1 /opt/share/apache2/htdocs/p1.before-restore-$TS 2>/dev/null
  mv /opt/share/apache2/htdocs/portfolio /opt/share/apache2/htdocs/portfolio.before-restore-$TS 2>/dev/null
  cp /opt/share/apache2/htdocs/index.html /opt/share/apache2/htdocs/index.html.before-restore-$TS 2>/dev/null
  cp /opt/etc/apache2/apache2.conf /opt/etc/apache2/apache2.conf.before-restore-$TS

  # 복구
  cp -a $STAGE/htdocs/p1 /opt/share/apache2/htdocs/
  cp -a $STAGE/htdocs/portfolio /opt/share/apache2/htdocs/
  cp -a $STAGE/htdocs/index.html /opt/share/apache2/htdocs/
  cp -a $STAGE/apache-conf/apache2.conf /opt/etc/apache2/apache2.conf
  chown -R root:root /opt/share/apache2/htdocs/{p1,portfolio,index.html}

  # Apache reload (graceful = 무중단)
  /opt/sbin/apachectl -k graceful
  echo restored
'
```

### B. 시연만 복구 (가장 흔한 케이스)

`/p1` 디렉토리만 위 절차의 해당 부분만 실행. apache2.conf는 그대로 두면 됨.

### C. 동작 확인

```bash
curl -I -k https://dsjh.synology.me:8443/p1/
curl -I -k https://dsjh.synology.me:8443/portfolio/
curl -I -k https://dsjh.synology.me:8443/
```

모두 `HTTP/1.1 200 OK` (또는 정상 리다이렉트)면 성공.

## 시연 데이터 (mock 어댑터가 처리하는 가짜 데이터)

박제본은 백엔드 없이 동작 — `frontend/src/lib/api/demo-mock.ts`가 axios adapter를 가로채 박제 시점 데이터 반환.

- 회원: `010-0000-0001` / `demo1234` → 김데모
- 강사: `instructor_demo` / `demo1234` → 박데모 강사
- 관리자: `admin_demo` / `demo1234` → 데모관리자

## 절대 금지 (D-008)

- 이 디렉토리의 NAS 박제용 산출물에 **추가 기능을 빌드해서 덮어쓰지 말 것**
- 박제는 "2026-05-16 시점의 동결본" 상태로만 보존
- 필라테스 메인 코드(`frontend/`)는 Railway 풀스택용으로 자유롭게 진화
- 박제용 빌드 명령은 더 이상 실행하지 않음 (`next.config.ts`의 export 모드는 잔재이나 손대지 않음)

## 다음 박제가 필요해질 경우?

원칙적으로 없다. 그러나 만약 필요하면 D-008 결정 자체를 먼저 재검토 후 진행.

## 관련 문서

- `DECISIONS.md` — D-006 (호스팅 전략), D-008 (NAS 박제 동결)
- `WORKLOG.md` — 2026-05-16 박제 스냅샷 기록
- 메모리 `reference_proto_snapshot.md` — 박제 사이트 운영 메모 (배포 시점 정보 풍부)
