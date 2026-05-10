// 정적 사이트 박제 스크립트 (Playwright)
// 사용법: cd frontend && node ../scripts/snapshot-pages.mjs

import { chromium } from "playwright";
import { mkdirSync, writeFileSync, existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, URL as NodeURL } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const OUT_DIR = join(ROOT, "snapshot", "p1");
const ASSET_DIR = OUT_DIR; // 자산도 같은 트리에 떨굼 (상대경로 그대로 작동)
const BASE = "http://localhost:3000";

// 풍부 데이터 계정
const ACCOUNTS = {
  member: { kind: "member", phone: "01000000001", password: "demo1234" },
  admin: { kind: "admin", loginId: "admin_demo", password: "demo1234" },
  instructor: { kind: "instructor", loginId: "instructor_demo", password: "demo1234" },
};

// 박제 대상 페이지 (역할 → 경로 목록)
const PAGES = {
  public: [
    "/login",
    "/admin-login",
    "/instructor-login",
    "/signup",
    "/reset-password",
  ],
  member: [
    "/home",
    "/schedule",
    "/reservations",
    "/membership",
    "/notifications",
    "/profile",
    "/attendance",
  ],
  admin: [
    "/dashboard",
    "/classes",
    "/instructors",
    "/members",
    "/membership-passes",
    "/settings",
    "/statistics",
  ],
  instructor: [
    "/instructor/schedule",
    "/instructor/attendance",
  ],
};

function ensureDir(filePath) {
  mkdirSync(dirname(filePath), { recursive: true });
}

// 경로 → 파일 시스템 경로
// "/login" -> "OUT_DIR/login.html"
// "/instructor/schedule" -> "OUT_DIR/instructor/schedule.html"
function pageOutPath(urlPath) {
  const clean = urlPath.replace(/^\//, "").replace(/\/$/, "") || "index";
  return join(OUT_DIR, `${clean}.html`);
}

// 자산 URL → 로컬 경로
// "http://localhost:3000/_next/static/chunks/abc.css" -> "OUT_DIR/_next/static/chunks/abc.css"
// "https://cdn.jsdelivr.net/..." -> 외부는 다운로드 안 함
function assetOutPath(urlString) {
  try {
    const u = new NodeURL(urlString);
    if (u.origin !== BASE) return null; // 외부는 그대로 둠 (인터넷 의존)
    const pathname = u.pathname;
    if (!pathname || pathname === "/") return null;
    // 쿼리스트링은 무시 (?favicon.xxx 등)
    return join(OUT_DIR, decodeURIComponent(pathname));
  } catch {
    return null;
  }
}

async function login(page, account) {
  if (account.kind === "member") {
    await page.goto(`${BASE}/login`);
    await page.waitForSelector('input[id="phone"]');
    await page.fill('input[id="phone"]', account.phone);
    await page.fill('input[id="password"]', account.password);
    await Promise.all([
      page.waitForURL("**/home", { timeout: 15000 }),
      page.click('button[type="submit"]'),
    ]);
  } else if (account.kind === "admin") {
    await page.goto(`${BASE}/admin-login`);
    await page.waitForSelector('input[id="loginId"]');
    await page.fill('input[id="loginId"]', account.loginId);
    await page.fill('input[id="password"]', account.password);
    await Promise.all([
      page.waitForURL("**/dashboard", { timeout: 15000 }),
      page.click('button[type="submit"]'),
    ]);
  } else if (account.kind === "instructor") {
    await page.goto(`${BASE}/instructor-login`);
    await page.waitForSelector('input[id="loginId"]');
    await page.fill('input[id="loginId"]', account.loginId);
    await page.fill('input[id="password"]', account.password);
    await Promise.all([
      page.waitForURL("**/instructor/schedule", { timeout: 15000 }),
      page.click('button[type="submit"]'),
    ]);
  }
}

async function snapshotPage(page, urlPath) {
  await page.goto(`${BASE}${urlPath}`, { waitUntil: "networkidle", timeout: 30000 });
  // 데이터 fetch 후 DOM 안정화를 위해 살짝 대기
  await page.waitForTimeout(800);

  // 페이지 DOM에서 script/preload 제거 후 outerHTML 추출
  const html = await page.evaluate(() => {
    const doc = document.documentElement.cloneNode(true);
    doc.querySelectorAll("script").forEach((s) => s.remove());
    doc.querySelectorAll('link[rel="preload"][as="script"]').forEach((l) => l.remove());
    doc.querySelectorAll("nextjs-portal, next-route-announcer, [data-nextjs-dialog-overlay]").forEach((el) => el.remove());
    doc.querySelectorAll('[role="region"][aria-live="polite"]').forEach((el) => el.remove());
    doc.querySelectorAll('[role="alert"][aria-live="assertive"]').forEach((el) => el.remove());
    doc.querySelectorAll('button[aria-label*="Next.js"]').forEach((el) => el.remove());

    // 모든 form의 action 제거 (정적 사이트에서 submit 못함)
    // 대신 onclick은 어차피 JS 제거되어 동작 안 함
    return "<!DOCTYPE html>\n" + doc.outerHTML;
  });

  // 절대경로 → 상대경로 (localhost:3000 제거)
  let cleaned = html.replace(/http:\/\/localhost:3000/g, "");

  const outPath = pageOutPath(urlPath);
  ensureDir(outPath);
  writeFileSync(outPath, cleaned, "utf-8");
  console.log(`  saved: ${urlPath} -> ${outPath} (${cleaned.length} bytes)`);
}

async function main() {
  mkdirSync(OUT_DIR, { recursive: true });

  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  // 자산 캡처 — 페이지가 로드하는 모든 same-origin 리소스를 파일로 저장
  const savedAssets = new Set();
  page.on("response", async (response) => {
    const url = response.url();
    const localPath = assetOutPath(url);
    if (!localPath) return;
    if (savedAssets.has(localPath)) return;
    if (!response.ok()) return;

    try {
      const buf = await response.body();
      ensureDir(localPath);
      writeFileSync(localPath, buf);
      savedAssets.add(localPath);
    } catch (e) {
      // body() 호출 실패 (3xx 리다이렉트 등) — 무시
    }
  });

  console.log("=== 공개 페이지 박제 ===");
  for (const p of PAGES.public) {
    await snapshotPage(page, p);
  }

  console.log("=== 회원 로그인 ===");
  await login(page, ACCOUNTS.member);
  console.log("=== 회원 페이지 박제 ===");
  for (const p of PAGES.member) {
    await snapshotPage(page, p);
  }

  console.log("=== 관리자 로그인 ===");
  await context.clearCookies();
  await page.evaluate(() => localStorage.clear()).catch(() => {});
  await login(page, ACCOUNTS.admin);
  console.log("=== 관리자 페이지 박제 ===");
  for (const p of PAGES.admin) {
    await snapshotPage(page, p);
  }

  console.log("=== 강사 로그인 ===");
  await context.clearCookies();
  await page.evaluate(() => localStorage.clear()).catch(() => {});
  await login(page, ACCOUNTS.instructor);
  console.log("=== 강사 페이지 박제 ===");
  for (const p of PAGES.instructor) {
    await snapshotPage(page, p);
  }

  // 강사 schedule 탭 변형 박제 (오늘은 위에서 박제됨, 내일/이번주 추가)
  console.log("=== 강사 schedule 탭 변형 박제 ===");
  const tabVariants = [
    { suffix: "tomorrow", label: "내일" },
    { suffix: "week", label: "이번 주" },
  ];
  for (const v of tabVariants) {
    await page.goto(`${BASE}/instructor/schedule`, { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(500);
    await page.evaluate((label) => {
      const buttons = Array.from(document.querySelectorAll("button"));
      const target = buttons.find((b) => b.textContent?.trim() === label);
      target?.click();
    }, v.label);
    await page.waitForTimeout(1500);
    const html = await page.evaluate(() => {
      const doc = document.documentElement.cloneNode(true);
      doc.querySelectorAll("script").forEach((s) => s.remove());
      doc.querySelectorAll('link[rel="preload"][as="script"]').forEach((l) => l.remove());
      doc.querySelectorAll("nextjs-portal, next-route-announcer, [data-nextjs-dialog-overlay]").forEach((el) => el.remove());
      doc.querySelectorAll('[role="region"][aria-live="polite"]').forEach((el) => el.remove());
      doc.querySelectorAll('[role="alert"][aria-live="assertive"]').forEach((el) => el.remove());
      doc.querySelectorAll('button[aria-label*="Next.js"]').forEach((el) => el.remove());
      return "<!DOCTYPE html>\n" + doc.outerHTML;
    });
    const cleaned = html.replace(/http:\/\/localhost:3000/g, "");
    const outPath = join(OUT_DIR, "instructor", `schedule-${v.suffix}.html`);
    ensureDir(outPath);
    writeFileSync(outPath, cleaned, "utf-8");
    console.log(`  saved: schedule-${v.suffix}.html (${cleaned.length} bytes)`);
  }

  console.log(`\n자산 ${savedAssets.size}개 저장됨`);
  console.log(`총 페이지 ${Object.values(PAGES).flat().length}개 박제 완료`);

  await browser.close();
}

main().catch((err) => {
  console.error("FAILED:", err);
  process.exit(1);
});
