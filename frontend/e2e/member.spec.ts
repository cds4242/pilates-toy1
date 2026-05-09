import { test, expect } from "@playwright/test";

test.describe("회원 페이지", () => {
  test("시나리오1: 홈 페이지 렌더링", async ({ page }) => {
    await page.goto("/home");
    await expect(page.locator("body")).toBeVisible();
    // Header 또는 콘텐츠 존재 확인
    const body = await page.textContent("body");
    expect(body).toBeTruthy();
  });

  test("시나리오2: 시간표 페이지 구조 렌더링", async ({ page }) => {
    await page.goto("/schedule");
    await expect(page.locator("h1", { hasText: "수업 시간표" })).toBeVisible();
  });
});
