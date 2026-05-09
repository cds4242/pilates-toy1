"use client";

import { useAuth } from "@/lib/hooks/use-auth";
import { ClassCard } from "@/components/design/ClassCard";

export default function InstructorSchedulePage() {
  const { user, logout } = useAuth();

  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center justify-between border-b border-[var(--color-border)]">
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">
          {user?.name || "강사"}님
        </h1>
        <button onClick={logout} className="text-[13px] text-[var(--color-text-sub)] hover:underline">
          로그아웃
        </button>
      </header>

      {/* Tabs */}
      <div className="flex border-b border-[var(--color-border)]">
        {["오늘", "내일", "이번 주"].map((tab, i) => (
          <button
            key={tab}
            className={`flex-1 py-3.5 text-[15px] text-center transition-colors ${
              i === 0
                ? "text-[var(--color-instructor)] font-semibold border-b-2 border-[var(--color-instructor)]"
                : "text-[var(--color-text-sub)]"
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* 수업 목록 */}
      <main className="p-6 flex flex-col gap-4">
        <div className="text-center py-16">
          <div className="text-[48px] mb-2">📋</div>
          <p className="text-[16px] font-semibold text-[var(--color-text-sub)]">
            오늘 배정된 수업이 없습니다
          </p>
        </div>
      </main>
    </div>
  );
}
