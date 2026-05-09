"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { format, addDays } from "date-fns";
import { useAuth } from "@/lib/hooks/use-auth";
import { api } from "@/lib/api/client";
import type { ClassSchedule } from "@/lib/types/domain";
import { formatTime } from "@/lib/utils/format";
import { Clock, User } from "lucide-react";

export default function InstructorSchedulePage() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const [activeTab, setActiveTab] = useState(0);
  const [schedules, setSchedules] = useState<ClassSchedule[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const today = new Date();
        let from: string, to: string;
        if (activeTab === 0) { from = to = format(today, "yyyy-MM-dd"); }
        else if (activeTab === 1) { const tmr = addDays(today, 1); from = to = format(tmr, "yyyy-MM-dd"); }
        else { from = format(today, "yyyy-MM-dd"); to = format(addDays(today, 6), "yyyy-MM-dd"); }
        const data = await api<ClassSchedule[]>("get", "/api/class-schedules", { from, to });
        setSchedules(data);
      } catch { /* empty */ }
      finally { setLoading(false); }
    }
    setLoading(true);
    load();
  }, [activeTab]);

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center justify-between border-b border-border">
        <h1 className="text-[20px] font-bold text-text-title">{user?.name || "강사"}님</h1>
        <button onClick={logout} className="text-[13px] text-text-sub hover:underline">로그아웃</button>
      </header>

      <div className="flex border-b border-border">
        {["오늘", "내일", "이번 주"].map((tab, i) => (
          <button key={tab} onClick={() => setActiveTab(i)}
            className={`flex-1 py-3.5 text-[15px] text-center transition-colors ${
              i === activeTab ? "text-instructor font-semibold border-b-2 border-instructor" : "text-text-sub"
            }`}>{tab}</button>
        ))}
      </div>

      <main className="p-6 flex flex-col gap-4">
        {loading ? (
          <div className="text-center py-16 text-text-sub">로딩 중...</div>
        ) : schedules.length === 0 ? (
          <div className="text-center py-16">
            <div className="text-[48px] mb-2">📋</div>
            <p className="text-[16px] font-semibold text-text-sub">배정된 수업이 없습니다</p>
          </div>
        ) : (
          schedules.map((cs) => (
            <div key={cs.id} className="rounded-[18px] border border-border bg-white p-5 flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-[16px] font-bold text-text-title">{formatTime(cs.startTime)}~{formatTime(cs.endTime)}</span>
                <span className="rounded-[20px] bg-pilates-light px-2.5 py-1 text-[13px] font-semibold text-pilates-dark">{cs.lessonTypeName}</span>
              </div>
              <p className="text-[15px] font-semibold text-text-title">{cs.instructorName}</p>
              <div className="flex gap-4 text-[13px] text-text-body">
                <span className="flex items-center gap-1"><User className="h-3.5 w-3.5" />{cs.currentCount}/{cs.maxCapacity}명</span>
                <span className="flex items-center gap-1"><Clock className="h-3.5 w-3.5" />50분</span>
              </div>
              <button onClick={() => router.push(`/instructor/attendance?classId=${cs.id}`)}
                className="w-full rounded-[8px] bg-instructor hover:bg-[#6A7DC2] py-3 text-[15px] font-semibold text-white transition-colors">
                출석 체크
              </button>
            </div>
          ))
        )}
      </main>
    </div>
  );
}
