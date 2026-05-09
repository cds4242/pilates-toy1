"use client";

import { MobileTabBar } from "@/components/design/MobileTabBar";

export default function MembershipPage() {
  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white pb-20">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 border-b border-[var(--color-border)]">
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">수강권</h1>
      </header>
      <main className="flex items-center justify-center min-h-[60vh]">
        <p className="text-[15px] text-[var(--color-text-sub)]">수강권 페이지 (v2 구현 예정)</p>
      </main>
      <MobileTabBar />
    </div>
  );
}
