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

      {/* 수업 목록 — 시안 구조: 수업 카드 리스트 */}
      <main className="p-6 flex flex-col gap-4">
        {/* 실제 API 연동 시 수업 데이터 표시. 현재는 시연용 빈 상태. */}
        <div className="rounded-[18px] border border-[var(--color-border)] bg-white p-5 flex flex-col gap-3 opacity-50">
          <div className="flex items-center justify-between">
            <span className="text-[16px] font-bold text-[var(--color-text-title)]">09:00~09:50</span>
            <span className="rounded-[20px] bg-[var(--color-pilates-light)] px-2.5 py-1 text-[13px] font-semibold text-[var(--color-pilates-dark)]">그룹 필라테스</span>
          </div>
          <p className="text-[15px] font-semibold text-[var(--color-text-title)]">스튜디오 A</p>
          <div className="flex gap-4 text-[13px] text-[var(--color-text-body)]">
            <span>5/8명</span><span>50분</span>
          </div>
          <button className="w-full rounded-[8px] bg-[var(--color-instructor)] py-3 text-[15px] font-semibold text-white">출석 체크</button>
        </div>
        <p className="text-center text-[13px] text-[var(--color-text-sub)]">수업 데이터는 백엔드 연동 시 표시됩니다</p>
      </main>
    </div>
  );
}
