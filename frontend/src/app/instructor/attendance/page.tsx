"use client";

import { useRouter } from "next/navigation";
import { ChevronLeft } from "lucide-react";

export default function InstructorAttendancePage() {
  const router = useRouter();

  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center gap-4 border-b border-[var(--color-border)]">
        <button onClick={() => router.back()} className="text-[var(--color-text-title)]">
          <ChevronLeft className="h-6 w-6" />
        </button>
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">출석 체크</h1>
      </header>

      {/* 수업 정보 바 */}
      <div className="bg-[var(--color-bg-section)] px-6 py-4 border-b border-[var(--color-border)] text-[13px] text-[var(--color-text-body)]">
        수업 정보 (출석 체크 페이지 — v2 구현 예정)
      </div>

      <main className="flex items-center justify-center min-h-[50vh]">
        <p className="text-[15px] text-[var(--color-text-sub)]">출석 체크 페이지 (v2 구현 예정)</p>
      </main>

      {/* 하단 버튼 */}
      <div className="sticky bottom-0 bg-white border-t border-[var(--color-border)] p-4">
        <button className="w-full bg-[var(--color-instructor)] text-white rounded-[8px] py-4 text-[16px] font-semibold">
          출석 체크 완료
        </button>
      </div>
    </div>
  );
}
