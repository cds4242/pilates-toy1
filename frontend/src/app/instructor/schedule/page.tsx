"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { format, addDays } from "date-fns";
import { useAuth } from "@/lib/hooks/use-auth";
import { api } from "@/lib/api/client";
import type { ClassSchedule } from "@/lib/types/domain";
import { formatTime } from "@/lib/utils/format";
import { Clock, User, ChevronDown, ChevronUp } from "lucide-react";

export default function InstructorSchedulePage() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const [activeTab, setActiveTab] = useState(0);
  const [schedules, setSchedules] = useState<ClassSchedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [weekOffset, setWeekOffset] = useState(0);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    async function load() {
      try {
        const today = new Date();
        let from: string, to: string;
        if (activeTab === 0) { from = to = format(today, "yyyy-MM-dd"); }
        else if (activeTab === 1) { const tmr = addDays(today, 1); from = to = format(tmr, "yyyy-MM-dd"); }
        else {
          const mondayOffset = -today.getDay() + 1 + weekOffset * 7;
          const weekStart = addDays(today, mondayOffset);
          from = format(weekStart, "yyyy-MM-dd");
          to = format(addDays(weekStart, 6), "yyyy-MM-dd");
        }
        const data = await api<ClassSchedule[]>("get", "/api/instructor/class-schedules", { from, to });
        setSchedules(data.sort((a, b) => a.classDate.localeCompare(b.classDate) || a.startTime.localeCompare(b.startTime)));
      } catch { /* empty */ }
      finally { setLoading(false); }
    }
    setLoading(true);
    load();
  }, [activeTab, weekOffset]);

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center justify-between border-b border-border">
        <div>
          <h1 className="text-[20px] font-bold text-text-title">{user?.name || "강사"}님</h1>
          {!loading && (
            <p className="text-[13px] text-text-sub mt-0.5">
              {activeTab === 2
                ? `이번 주 ${schedules.length}건의 수업`
                : `오늘 ${schedules.filter(s => s.classDate === format(new Date(), "yyyy-MM-dd")).length}건의 수업`}
            </p>
          )}
        </div>
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

      {activeTab === 2 && (
        <div className="flex items-center justify-center gap-4 py-2 border-b border-border">
          <button onClick={() => setWeekOffset(weekOffset - 1)} className="text-text-sub hover:text-text-body">← 지난주</button>
          {weekOffset !== 0 && (
            <button onClick={() => setWeekOffset(0)} className="text-[12px] text-pilates-dark font-semibold">이번 주</button>
          )}
          <button onClick={() => setWeekOffset(weekOffset + 1)} className="text-text-sub hover:text-text-body">다음주 →</button>
        </div>
      )}

      {!loading && activeTab === 0 && schedules.length > 0 && (
        <div className="flex gap-3 px-6 pt-4">
          <div className="flex-1 rounded-[12px] bg-white border border-border p-3 text-center card-elevated">
            <p className="text-[18px] font-bold text-text-title">{schedules.length}</p>
            <p className="text-[11px] text-text-sub">오늘 수업</p>
          </div>
          <div className="flex-1 rounded-[12px] bg-white border border-border p-3 text-center card-elevated">
            <p className="text-[18px] font-bold text-text-title">{schedules.reduce((sum, s) => sum + s.currentCount, 0)}</p>
            <p className="text-[11px] text-text-sub">총 수강생</p>
          </div>
          <div className="flex-1 rounded-[12px] bg-white border border-border p-3 text-center card-elevated">
            <p className="text-[18px] font-bold text-instructor">{schedules.filter(s => s.currentCount >= s.maxCapacity).length}</p>
            <p className="text-[11px] text-text-sub">마감 수업</p>
          </div>
        </div>
      )}

      <main className="p-6 flex flex-col gap-4">
        {loading ? (
          <div className="text-center py-16 text-text-sub">로딩 중...</div>
        ) : schedules.length === 0 ? (
          <div className="text-center py-16 animate-fade-in">
            <div className="text-[48px] mb-3">☕</div>
            <p className="text-[16px] font-semibold text-text-title mb-1">오늘은 배정된 수업이 없어요</p>
            <p className="text-[14px] text-text-sub">편하게 쉬세요! 내일 수업을 확인하려면<br/>&quot;내일&quot; 탭을 눌러보세요.</p>
            <p className="text-[13px] text-text-sub mt-2">내일 탭에서 내일 수업을 확인해보세요</p>
          </div>
        ) : (
          <>
            {activeTab === 2 && schedules.length > 0 && (
              <div className="rounded-[12px] bg-white border border-border p-4 mb-4">
                <p className="text-[13px] font-bold text-text-title mb-2">주간 요약</p>
                <div className="flex justify-between text-center">
                  {["월","화","수","목","금","토","일"].map((day, i) => {
                    const dayCount = schedules.filter(s => new Date(s.classDate + "T00:00:00").getDay() === (i === 6 ? 0 : i + 1)).length;
                    return (
                      <div key={day} className="flex-1">
                        <p className="text-[11px] text-text-sub">{day}</p>
                        <p className={`text-[15px] font-bold ${dayCount > 0 ? "text-instructor" : "text-text-sub"}`}>{dayCount}</p>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
            {schedules.map((cs, idx) => {
              const showDateHeader = activeTab === 2 && (idx === 0 || schedules[idx - 1].classDate !== cs.classDate);
              const dayNames = ["일", "월", "화", "수", "목", "금", "토"];
              const dateLabel = showDateHeader ? (() => {
                const d = new Date(cs.classDate + "T00:00:00");
                return `${d.getMonth() + 1}/${d.getDate()} (${dayNames[d.getDay()]})`;
              })() : null;
              const now = new Date();
              const nowStr = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
              const isActive = cs.classDate === format(now, "yyyy-MM-dd") && cs.startTime <= nowStr && cs.endTime > nowStr;
              const remaining = cs.maxCapacity - cs.currentCount;
              const isExpanded = expandedId === cs.id;
              return (<div key={cs.id}>
              {dateLabel && <h3 className="text-[15px] font-bold text-text-title mt-2 mb-2">{dateLabel}</h3>}
              <div className={`rounded-[18px] border border-border bg-white p-5 flex flex-col gap-3 card-elevated card-hover ${isActive ? "ring-2 ring-green-400" : ""}`}>
                <div className="cursor-pointer" onClick={() => setExpandedId(isExpanded ? null : cs.id)}>
                  <div className="flex items-center justify-between">
                    <span className="text-[16px] font-bold text-text-title">{formatTime(cs.startTime)}~{formatTime(cs.endTime)}</span>
                    <div className="flex items-center gap-2">
                      {isActive && <span className="text-[11px] text-green-600 font-semibold">● 진행 중</span>}
                      <span className="rounded-[20px] bg-pilates-light px-2.5 py-1 text-[13px] font-semibold text-pilates-dark">{cs.lessonTypeName}</span>
                      {isExpanded ? <ChevronUp className="h-4 w-4 text-text-sub" /> : <ChevronDown className="h-4 w-4 text-text-sub" />}
                    </div>
                  </div>
                  <p className="text-[15px] font-semibold text-text-title mt-3">{cs.instructorName}</p>
                  <div className="flex flex-col gap-1 mt-3">
                    <div className="flex gap-4 text-[13px] text-text-body">
                      <span className="flex items-center gap-1"><User className="h-3.5 w-3.5" />{cs.currentCount > 0 ? `${cs.currentCount}명 예약 (잔여 ${remaining}석)` : ""}</span>
                      <span className="flex items-center gap-1"><Clock className="h-3.5 w-3.5" />50분</span>
                    </div>
                    {cs.currentCount === 0 && (
                      <p className="text-[12px] text-text-sub ml-0.5">아직 예약이 없습니다</p>
                    )}
                  </div>
                </div>
                {isExpanded && (
                  <div className="border-t border-border pt-3 mt-3">
                    <div className="grid grid-cols-2 gap-2 text-[13px] mb-3">
                      <div>
                        <p className="text-text-sub">수업 유형</p>
                        <p className="font-semibold text-text-title">{cs.lessonTypeName}</p>
                      </div>
                      <div>
                        <p className="text-text-sub">수업 시간</p>
                        <p className="font-semibold text-text-title">50분</p>
                      </div>
                      <div>
                        <p className="text-text-sub">예약 현황</p>
                        <p className="font-semibold text-text-title">{cs.currentCount}명 / {cs.maxCapacity}명</p>
                      </div>
                      <div>
                        <p className="text-text-sub">잔여석</p>
                        <p className={`font-semibold ${cs.maxCapacity - cs.currentCount <= 2 ? "text-[var(--color-error)]" : "text-text-title"}`}>{cs.maxCapacity - cs.currentCount}석</p>
                      </div>
                    </div>
                    {cs.currentCount === 0 && (
                      <p className="text-[13px] text-text-sub italic mb-2">아직 예약한 회원이 없습니다</p>
                    )}
                    {cs.currentCount > 0 && (
                      <p className="text-[12px] text-text-sub">출석 체크 버튼을 눌러 수강생 출석을 확인하세요</p>
                    )}
                  </div>
                )}
                <button onClick={(e) => { e.stopPropagation(); router.push(`/instructor/attendance?classId=${cs.id}`); }}
                  disabled={cs.currentCount === 0}
                  className={`w-full rounded-[8px] py-3 text-[15px] font-semibold transition-colors ${cs.currentCount === 0 ? "bg-[var(--color-bg-section)] text-[var(--color-text-sub)] cursor-not-allowed" : "bg-gradient-to-r from-[var(--color-instructor)] to-[#6A7DC2] text-white"}`}>
                  {cs.currentCount === 0 ? "예약 없음" : "출석 체크"}
                </button>
              </div>
              </div>);
            })}
          </>
        )}
      </main>
    </div>
  );
}
