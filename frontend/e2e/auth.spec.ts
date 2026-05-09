import { test, expect } from "@playwright/test";

test.describe("인증 플로우", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/login");
    await page.evaluate(() => localStorage.clear());
  });

  test("시나리오1: 로그인 페이지 렌더링", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByText("Pilates Studio").first()).toBeVisible();
    await expect(page.getByRole("tab", { name: "회원" })).toBeVisible();
    await expect(page.getByRole("tab", { name: "관리자" })).toBeVisible();
    await expect(page.getByRole("button", { name: "로그인" })).toBeVisible();
  });

  test("시나리오2: 회원가입 페이지 렌더링 + 단계 표시", async ({ page }) => {
    await page.goto("/signup");
    await expect(page.getByText("회원가입").first()).toBeVisible();
    await expect(page.getByPlaceholder("01012345678")).toBeVisible();
  });

  test("시나리오3: 미인증 시 루트 → 로그인 리디렉트", async ({ page }) => {
    await page.goto("/");
    await page.waitForURL("**/login", { timeout: 5000 });
    await expect(page).toHaveURL(/\/login/);
  });
});
