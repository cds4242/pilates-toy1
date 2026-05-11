// 박제 결과물 후처리: 링크/자산 경로를 /p1 prefix로 치환
// 사용법: node snapshot-rewrite.mjs

import { readFileSync, writeFileSync, readdirSync, statSync, unlinkSync, existsSync } from "node:fs";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const OUT_DIR = join(ROOT, "snapshot", "p1");
const PREFIX = "/p1";

// 박제한 페이지 경로 목록 (snapshot-pages.mjs와 동일)
const PAGE_PATHS = [
  "/login", "/admin-login", "/instructor-login", "/signup", "/reset-password",
  "/home", "/schedule", "/timetable", "/reservations", "/membership", "/notifications", "/profile", "/attendance",
  "/dashboard", "/classes", "/instructors", "/members", "/membership-passes", "/settings", "/statistics",
  "/instructor/schedule", "/instructor/attendance",
];

// 페이지 경로 정규식 (긴 것 먼저, /instructor/schedule이 /schedule보다 먼저 매칭되도록)
const sortedPagePaths = [...PAGE_PATHS].sort((a, b) => b.length - a.length);

function walk(dir) {
  const out = [];
  for (const name of readdirSync(dir)) {
    const full = join(dir, name);
    const st = statSync(full);
    if (st.isDirectory()) {
      out.push(...walk(full));
    } else {
      out.push(full);
    }
  }
  return out;
}

// 1. 불필요한 파일 정리
console.log("=== 불필요 파일 정리 ===");
let deleted = 0;

// 1-1. 확장자 없는 페이지 응답 파일 삭제 (admin-login, home, dashboard 등)
//      대응되는 .html이 같은 디렉토리에 있으면 삭제
for (const p of PAGE_PATHS) {
  const clean = p.replace(/^\//, "");
  const noExt = join(OUT_DIR, clean);
  const html = join(OUT_DIR, `${clean}.html`);
  if (existsSync(noExt) && existsSync(html)) {
    const st = statSync(noExt);
    if (st.isFile()) {
      unlinkSync(noExt);
      deleted++;
    }
  }
}

// 1-2. _next 안의 .js 파일 모두 삭제 (스크립트 다 제거했으니 불필요)
function rmJsRecursive(dir) {
  if (!existsSync(dir)) return;
  for (const name of readdirSync(dir)) {
    const full = join(dir, name);
    const st = statSync(full);
    if (st.isDirectory()) rmJsRecursive(full);
    else if (full.endsWith(".js")) {
      unlinkSync(full);
      deleted++;
    }
  }
}
rmJsRecursive(join(OUT_DIR, "_next"));
console.log(`  삭제됨: ${deleted}개 파일`);

// 2. HTML 파일들의 경로 치환
console.log("=== HTML 경로 치환 ===");
const htmlFiles = walk(OUT_DIR).filter((f) => f.endsWith(".html"));

let totalRewrites = 0;
for (const file of htmlFiles) {
  let html = readFileSync(file, "utf-8");
  const before = html.length;

  // 2-1. 페이지 간 링크: href="/login" → href="/p1/login.html"
  //      (긴 경로부터 매칭)
  for (const p of sortedPagePaths) {
    // href="/login" 또는 href="/login?xxx" 같은 형태 매칭
    // 다른 경로 prefix와 충돌 방지를 위해 closing quote/?/#를 lookahead로 체크
    const escaped = p.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const re = new RegExp(`href="${escaped}(?=["?#/]|$)`, "g");
    html = html.replace(re, `href="${PREFIX}${p}.html`);
  }

  // 2-2. 자산 경로: /_next/, /studio.jpg, /favicon.ico, /__nextjs_font/ 등
  //      페이지 link/preload/img 등의 src/href 모두
  // 페이지 경로(이미 /p1/...html로 바뀐 것들)는 건드리지 않음
  // 단순한 접근: src="/xxx" 와 href="/xxx" 중 /p1으로 시작 안 하는 것들에 prefix 추가
  // url(/__nextjs_font/...) 같은 CSS 인라인 폰트도 처리
  html = html.replace(/(src|href)="\/(?!p1\/)([^"]*)"/g, (m, attr, rest) => {
    // 외부 URL은 이미 https:// 로 시작하니 여기 안 잡힘
    return `${attr}="${PREFIX}/${rest}"`;
  });
  // CSS 안의 url(/__nextjs_font/...)
  html = html.replace(/url\(\/(?!p1\/)([^)]+)\)/g, (m, rest) => `url(${PREFIX}/${rest})`);

  if (html.length !== before || html !== readFileSync(file, "utf-8")) {
    writeFileSync(file, html, "utf-8");
    totalRewrites++;
  }
}
console.log(`  치환됨: ${totalRewrites}/${htmlFiles.length}개 HTML`);

// 2-3. 로그인 form에 action 추가 (JS 제거되어 submit이 안되므로)
//      <form ...> → <form action="..." ...>  (브라우저 기본 동작으로 GET 이동)
const LOGIN_TARGETS = {
  "login.html": `${PREFIX}/home.html`,
  "admin-login.html": `${PREFIX}/dashboard.html`,
  "instructor-login.html": `${PREFIX}/instructor/schedule.html`,
};
for (const [filename, target] of Object.entries(LOGIN_TARGETS)) {
  const file = join(OUT_DIR, filename);
  if (!existsSync(file)) continue;
  let html = readFileSync(file, "utf-8");
  // 첫 번째 <form ...> 태그에만 action 박음. action 이미 있으면 교체.
  let replaced = false;
  html = html.replace(/<form\b([^>]*)>/, (m, attrs) => {
    if (replaced) return m;
    replaced = true;
    // 기존 action 제거 후 새로 박음, method=GET로 단순 이동
    const cleaned = attrs.replace(/\s*action="[^"]*"/g, "").replace(/\s*method="[^"]*"/gi, "");
    return `<form action="${target}" method="get"${cleaned}>`;
  });
  // password 인풋의 name을 빼서 쿼리스트링에 password 노출되지 않게 처리
  // (id="password" 인풋의 name 속성 제거 — name 없으면 form submit 시 전송 안됨)
  html = html.replace(/<input([^>]*\bid="password"[^>]*)>/g, (m, attrs) => {
    const cleaned = attrs.replace(/\s*name="[^"]*"/g, "");
    return `<input${cleaned}>`;
  });
  // phone/loginId도 마찬가지로 name 제거 (URL에 안 노출되는 게 깔끔)
  html = html.replace(/<input([^>]*\bid="(?:phone|loginId)"[^>]*)>/g, (m, attrs) => {
    const cleaned = attrs.replace(/\s*name="[^"]*"/g, "");
    return `<input${cleaned}>`;
  });
  writeFileSync(file, html, "utf-8");
  console.log(`  로그인 form action 설정: ${filename} -> ${target}`);
}

// 2-4. 로그아웃 버튼 → 로그인 페이지 링크
//      페이지별로 어디 로그인 페이지로 갈지 결정
const LOGOUT_TARGETS = {
  // 회원 페이지들 → /p1/login.html
  "home.html": `${PREFIX}/login.html`,
  "schedule.html": `${PREFIX}/login.html`,
  "reservations.html": `${PREFIX}/login.html`,
  "membership.html": `${PREFIX}/login.html`,
  "notifications.html": `${PREFIX}/login.html`,
  "profile.html": `${PREFIX}/login.html`,
  "attendance.html": `${PREFIX}/login.html`,
  // 관리자 페이지들 → /p1/admin-login.html
  "dashboard.html": `${PREFIX}/admin-login.html`,
  "classes.html": `${PREFIX}/admin-login.html`,
  "instructors.html": `${PREFIX}/admin-login.html`,
  "members.html": `${PREFIX}/admin-login.html`,
  "members-expiring.html": `${PREFIX}/admin-login.html`,
  "membership-passes.html": `${PREFIX}/admin-login.html`,
  "settings.html": `${PREFIX}/admin-login.html`,
  "statistics.html": `${PREFIX}/admin-login.html`,
  // 강사 페이지들 → /p1/instructor-login.html
  "instructor/schedule.html": `${PREFIX}/instructor-login.html`,
  "instructor/schedule-tomorrow.html": `${PREFIX}/instructor-login.html`,
  "instructor/schedule-week.html": `${PREFIX}/instructor-login.html`,
  "instructor/attendance.html": `${PREFIX}/instructor-login.html`,
};

// "로그아웃" 들어간 button 태그를 a 태그로 교체
// 두 가지 패턴 처리:
//  (1) aria-label="로그아웃"으로 식별되는 button (관리자 sidebar)
//  (2) 안에 "로그아웃" 텍스트가 들어있는 button (회원 profile, 강사)
function rewriteLogoutButton(html, targetUrl) {
  let modified = html;

  // 패턴 1: <button aria-label="로그아웃" ...>...inner...</button>
  // button을 a로 바꾸되 attrs는 거의 그대로 유지
  modified = modified.replace(
    /<button\b([^>]*\baria-label="로그아웃"[^>]*)>([\s\S]*?)<\/button>/,
    (m, attrs, inner) => {
      const cleanedAttrs = attrs
        .replace(/\s*type="[^"]*"/g, "")
        .replace(/\s*disabled(?:="[^"]*")?/g, "");
      return `<a href="${targetUrl}"${cleanedAttrs} style="display:inline-flex;align-items:center;justify-content:center;text-decoration:none">${inner}</a>`;
    },
  );

  // 패턴 2: <button ...>...로그아웃...</button> (aria-label 없는 케이스)
  // [\s\S]*? 비탐욕으로 가장 가까운 </button>까지
  modified = modified.replace(
    /<button\b((?:(?!aria-label="로그아웃")[^>])*)>((?:(?!<\/button>)[\s\S])*?로그아웃(?:(?!<\/button>)[\s\S])*?)<\/button>/,
    (m, attrs, inner) => {
      const cleanedAttrs = attrs
        .replace(/\s*type="[^"]*"/g, "")
        .replace(/\s*disabled(?:="[^"]*")?/g, "");
      return `<a href="${targetUrl}"${cleanedAttrs} style="display:inline-flex;align-items:center;text-decoration:none">${inner}</a>`;
    },
  );

  return modified;
}

let logoutCount = 0;
for (const [relPath, target] of Object.entries(LOGOUT_TARGETS)) {
  const file = join(OUT_DIR, relPath);
  if (!existsSync(file)) continue;
  let html = readFileSync(file, "utf-8");
  const before = html;
  html = rewriteLogoutButton(html, target);
  if (html !== before) {
    writeFileSync(file, html, "utf-8");
    logoutCount++;
  }
}
console.log(`  로그아웃 버튼 링크화: ${logoutCount}개 페이지`);

// 2-5. 강사 schedule 탭 (오늘/내일/이번 주) → 페이지 이동 링크
//      각 파일에서 "오늘"/"내일"/"이번 주" 버튼을 a 태그로 교체
const SCHEDULE_TAB_FILES = [
  "instructor/schedule.html",
  "instructor/schedule-tomorrow.html",
  "instructor/schedule-week.html",
];
const SCHEDULE_TAB_TARGETS = [
  { label: "오늘", url: `${PREFIX}/instructor/schedule.html` },
  { label: "내일", url: `${PREFIX}/instructor/schedule-tomorrow.html` },
  { label: "이번 주", url: `${PREFIX}/instructor/schedule-week.html` },
];

for (const relPath of SCHEDULE_TAB_FILES) {
  const file = join(OUT_DIR, relPath);
  if (!existsSync(file)) continue;
  let html = readFileSync(file, "utf-8");
  let modified = false;
  for (const tab of SCHEDULE_TAB_TARGETS) {
    // <button ...>오늘</button> 같은 단순 매칭. inner에 정확히 그 텍스트만 있는 케이스
    // (탭 버튼은 "오늘", "내일", "이번 주"만 텍스트로 들어감)
    const escaped = tab.label.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const re = new RegExp(`<button\\b([^>]*)>${escaped}</button>`);
    const newHtml = html.replace(re, (m, attrs) => {
      const cleanedAttrs = attrs
        .replace(/\s*type="[^"]*"/g, "")
        .replace(/\s*disabled(?:="[^"]*")?/g, "");
      return `<a href="${tab.url}"${cleanedAttrs} style="display:inline-flex;align-items:center;justify-content:center;text-decoration:none">${tab.label}</a>`;
    });
    if (newHtml !== html) {
      html = newHtml;
      modified = true;
    }
  }
  if (modified) {
    writeFileSync(file, html, "utf-8");
    console.log(`  탭 링크화: ${relPath}`);
  }
}

// 2-6. dashboard의 만료 임박 카드 링크를 별도 박제본 members-expiring.html로 치환
//      (위 일반 치환에서는 /p1/members.html?quick=expiring이 되므로 별도 처리)
{
  const file = join(OUT_DIR, "dashboard.html");
  if (existsSync(file)) {
    let html = readFileSync(file, "utf-8");
    const before = html;
    html = html.replace(
      /href="\/p1\/members\.html\?quick=expiring"/g,
      `href="${PREFIX}/members-expiring.html"`,
    );
    if (html !== before) {
      writeFileSync(file, html, "utf-8");
      console.log(`  dashboard.html: 만료 임박 링크 → /p1/members-expiring.html`);
    }
  }
}

// 3. CSS 파일 안의 url(/__nextjs_font/...) 도 치환
console.log("=== CSS 경로 치환 ===");
const cssFiles = walk(OUT_DIR).filter((f) => f.endsWith(".css"));
for (const file of cssFiles) {
  let css = readFileSync(file, "utf-8");
  const before = css;
  css = css.replace(/url\(\/(?!p1\/)([^)]+)\)/g, (m, rest) => `url(${PREFIX}/${rest})`);
  if (css !== before) {
    writeFileSync(file, css, "utf-8");
    console.log(`  ${file}`);
  }
}

console.log("\n완료");
