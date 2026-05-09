import { test, expect } from "@playwright/test";

test.describe("관리자 페이지", () => {
  test("시나리오1: 대시보드 페이지 렌더링", async ({ page }) => {
    await page.goto("/dashboard");
    await expect(page.locator("h1", { hasText: "대시보드" })).toBeVisible();
  });

  test("시나리오2: 관리자 사이드바 네비게이션 (데스크톱)", async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 });
    await page.goto("/dashboard");
    await expect(page.locator("aside")).toBeVisible();
    await expect(page.getByText("회원 관리")).toBeVisible();
    await expect(page.getByText("수업 관리")).toBeVisible();
  });
});
