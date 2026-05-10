/* eslint-disable */
import { chromium, devices } from "playwright";
import path from "path";
import fs from "fs/promises";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUT_DIR = path.join(__dirname, "screenshots");
const BASE = process.env.BASE_URL || "http://localhost:3000";

const MOBILE_VIEWPORT = { width: 390, height: 844 };
const DESKTOP_VIEWPORT = { width: 1440, height: 900 };

const MEMBER_LOGIN = { phone: "010-9000-1000", password: "Test1234!" };
const INSTRUCTOR_LOGIN = { id: "instructor1", password: "admin1234" };
const ADMIN_LOGIN = { id: "admin", password: "admin1234" };

// Targets to capture. tabIndex applies for instructor schedule (0=오늘 / 1=내일 / 2=이번 주)
const captures = [
  { kind: "mobile", name: "01_login_member", url: "/login", auth: null, wait: 800 },
  { kind: "mobile", name: "02_signup", url: "/signup", auth: null, wait: 600 },
  { kind: "mobile", name: "03_login_instructor", url: "/instructor-login", auth: null, wait: 600 },
  { kind: "mobile", name: "04_login_admin", url: "/admin-login", auth: null, wait: 600 },
  { kind: "mobile", name: "10_member_home", url: "/home", auth: "member", wait: 1500 },
  { kind: "mobile", name: "11_member_schedule", url: "/schedule", auth: "member", wait: 1500 },
  { kind: "mobile", name: "12_member_membership", url: "/membership", auth: "member", wait: 1500 },
  { kind: "mobile", name: "13_member_reservations", url: "/reservations", auth: "member", wait: 1500 },
  { kind: "mobile", name: "14_member_attendance", url: "/attendance", auth: "member", wait: 1500 },
  { kind: "mobile", name: "15_member_profile", url: "/profile", auth: "member", wait: 1500 },
  // Instructor — 3 views
  { kind: "mobile", name: "20_instructor_today", url: "/instructor/schedule", auth: "instructor", wait: 1500, tabIndex: 0 },
  { kind: "mobile", name: "21_instructor_tomorrow", url: "/instructor/schedule", auth: "instructor", wait: 1500, tabIndex: 1 },
  { kind: "mobile", name: "22_instructor_week", url: "/instructor/schedule", auth: "instructor", wait: 1500, tabIndex: 2 },
  // Admin
  { kind: "desktop", name: "30_admin_dashboard", url: "/dashboard", auth: "admin", wait: 2500 },
  { kind: "desktop", name: "31_admin_members", url: "/members", auth: "admin", wait: 2000 },
  { kind: "desktop", name: "32_admin_instructors", url: "/instructors", auth: "admin", wait: 2000 },
  { kind: "desktop", name: "33_admin_classes", url: "/classes", auth: "admin", wait: 2000 },
  { kind: "desktop", name: "34_admin_membership_passes", url: "/membership-passes", auth: "admin", wait: 1500 },
  { kind: "desktop", name: "35_admin_settings", url: "/settings", auth: "admin", wait: 1500 },
  { kind: "desktop", name: "40_admin_login_pc", url: "/admin-login", auth: null, wait: 800 },
  { kind: "desktop", name: "41_member_login_pc", url: "/login", auth: null, wait: 800 },
  { kind: "desktop", name: "42_instructor_login_pc", url: "/instructor-login", auth: null, wait: 800 },
];

async function memberLogin(page) {
  await page.goto(`${BASE}/login`);
  await page.waitForTimeout(600);
  await page.locator('input[id="phone"]').first().fill(MEMBER_LOGIN.phone);
  await page.locator('input[id="password"]').first().fill(MEMBER_LOGIN.password);
  await page.locator('button[type="submit"]').first().click();
  await page.waitForURL(/\/home/, { timeout: 10000 });
  await page.waitForTimeout(1200);
}

async function instructorLogin(page) {
  await page.goto(`${BASE}/instructor-login`);
  await page.waitForTimeout(500);
  await page.locator('input[id="loginId"]').fill(INSTRUCTOR_LOGIN.id);
  await page.locator('input[id="password"]').fill(INSTRUCTOR_LOGIN.password);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL(/\/instructor\/schedule/, { timeout: 10000 });
  await page.waitForTimeout(1200);
}

async function adminLogin(page) {
  await page.goto(`${BASE}/admin-login`);
  await page.waitForTimeout(500);
  await page.locator('input[id="loginId"]').fill(ADMIN_LOGIN.id);
  await page.locator('input[id="password"]').fill(ADMIN_LOGIN.password);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL(/\/dashboard/, { timeout: 10000 });
  await page.waitForTimeout(2000);
}

async function ensureAuth(page, kind) {
  if (!kind) return;
  if (kind === "member") await memberLogin(page);
  else if (kind === "instructor") await instructorLogin(page);
  else if (kind === "admin") await adminLogin(page);
}

async function main() {
  await fs.mkdir(OUT_DIR, { recursive: true });
  const browser = await chromium.launch({ headless: true });

  // Group by (kind, auth) so we share contexts/login
  const groups = new Map();
  for (const c of captures) {
    const key = `${c.kind}__${c.auth ?? "none"}`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(c);
  }

  for (const [key, items] of groups) {
    const [kind, auth] = key.split("__");
    const viewport = kind === "mobile" ? MOBILE_VIEWPORT : DESKTOP_VIEWPORT;
    const ctx = await browser.newContext({
      ...(kind === "mobile" ? devices["iPhone 13"] : {}),
      viewport,
      isMobile: kind === "mobile",
      hasTouch: kind === "mobile",
      deviceScaleFactor: 2,
      colorScheme: "light",
    });
    const page = await ctx.newPage();
    if (auth !== "none") {
      console.log(`[${key}] login...`);
      await ensureAuth(page, auth);
    }
    for (const cap of items) {
      const fp = path.join(OUT_DIR, `${cap.name}.png`);
      console.log(`  ${cap.name} -> ${cap.url}${cap.tabIndex !== undefined ? ` (tab ${cap.tabIndex})` : ""}`);
      try {
        await page.goto(`${BASE}${cap.url}`);
        await page.waitForTimeout(cap.wait);

        // Click instructor tab if requested
        if (cap.tabIndex !== undefined) {
          const tabLabels = ["오늘", "내일", "이번 주"];
          await page.getByRole("button", { name: tabLabels[cap.tabIndex] }).click();
          await page.waitForTimeout(1200);
        }

        try {
          await page.addStyleTag({ content: '[data-sonner-toaster]{display:none!important;}' });
        } catch (_) {}
        await page.screenshot({ path: fp, fullPage: false });
      } catch (err) {
        console.error(`  failed ${cap.name}:`, err.message);
      }
    }
    await ctx.close();
  }
  await browser.close();
  console.log(`\nDone. Screenshots in ${OUT_DIR}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
