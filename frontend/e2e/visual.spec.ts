import { test, expect } from "@playwright/test";

const pages = [
  { name: "member-login", url: "/login" },
  { name: "member-signup", url: "/signup" },
  { name: "member-home", url: "/home" },
  { name: "member-schedule", url: "/schedule" },
  { name: "member-profile", url: "/profile" },
  { name: "member-membership", url: "/membership" },
  { name: "member-reservations", url: "/reservations" },
  { name: "admin-dashboard", url: "/dashboard" },
  { name: "admin-members", url: "/members" },
  { name: "admin-classes", url: "/classes" },
  { name: "instructor-schedule", url: "/instructor/schedule" },
  { name: "instructor-attendance", url: "/instructor/attendance" },
];

test.describe("시각 회귀 테스트", () => {
  for (const page of pages) {
    test(`${page.name} 스크린샷 baseline`, async ({ page: p }) => {
      await p.goto(page.url);
      await p.waitForTimeout(500);
      await expect(p).toHaveScreenshot(`${page.name}.png`, {
        maxDiffPixelRatio: 0.05,
      });
    });
  }
});
