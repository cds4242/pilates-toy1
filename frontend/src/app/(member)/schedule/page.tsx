"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { format, addDays } from "date-fns";
import { ko } from "date-fns/locale";
import Link from "next/link";
import { CalendarDays, ChevronLeft, Clock, User } from "lucide-react";
import { classroomApi } from "@/lib/api/classroom";
import { reservationApi } from "@/lib/api/reservation";
import { memberApi } from "@/lib/api/member";
import type { ClassSchedule, Membership, Reservation } from "@/lib/types/domain";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { formatTime } from "@/lib/utils/format";
import { PageGuide } from "@/components/design/HelpTip";

const lessonDesc: Record<string, string> = {
  "개인": "1:1 맞춤 레슨",
  "듀엣": "2인 소그룹 레슨",
  "그룹": "최대 8인 그룹 수업",
  "체험": "첫 방문 체험 수업",
};

const lessonColor: Record<string, string> = {
  "개인": "bg-blue-400",
  "듀엣": "bg-purple-400",
  "그룹": "bg-green-400",
  "체험": "bg-amber-400",
};

export default function SchedulePage() {
  const router = useRouter();
  const [schedules, setSchedules] = useState<ClassSchedule[]>([]);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [myReservations, setMyReservations] = useState<Reservation[]>([]);
  const [selectedDate, setSelectedDate] = useState(new Date());

  useEffect(() => {
    if (typeof window === "undefined") return;
    const params = new URLSearchParams(window.location.search);
    const dateParam = params.get("date");
    if (dateParam && /^\d{4}-\d{2}-\d{2}$/.test(dateParam)) {
      const d = new Date(`${dateParam}T00:00:00`);
      if (!Number.isNaN(d.getTime())) setSelectedDate(d);
    }
  }, []);
  const [loading, setLoading] = useState(true);
  const [reserving, setReserving] = useState<number | null>(null);
  const [confirmClassId, setConfirmClassId] = useState<number | null>(null);

  const dates = Array.from({ length: 14 }, (_, i) => addDays(new Date(), i));
  const from = format(dates[0], "yyyy-MM-dd");
  const to = format(dates[13], "yyyy-MM-dd");

  useEffect(() => {
    async function load() {
      try {
        const [data, ms, rs] = await Promise.all([
          classroomApi.getSchedules(from, to),
          memberApi.getMemberships(),
          memberApi.getReservations(),
        ]);
        setSchedules(data);
        setMemberships(ms);
        setMyReservations(rs);
      } catch {
        // empty
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [from, to]);

  const hasActiveMembership = memberships.some((m) => m.status === "ACTIVE");
  const selectedDateStr = format(selectedDate, "yyyy-MM-dd");
  const now = new Date();
  const isToday = selectedDateStr === format(now, "yyyy-MM-dd");
  const daySchedules = schedules
    .filter((s) => s.classDate === selectedDateStr)
    .sort((a, b) => {
      if (isToday) {
        const aStart = new Date(`${a.classDate}T${a.endTime}`);
        const bStart = new Date(`${b.classDate}T${b.endTime}`);
        const aPast = aStart <= now;
        const bPast = bStart <= now;
        if (aPast !== bPast) return aPast ? 1 : -1;
      }
      return a.startTime.localeCompare(b.startTime);
    });

  const handleReserve = async (classScheduleId: number) => {
    setReserving(classScheduleId);
    try {
      await reservationApi.create(classScheduleId);
      toast.success("예약이 완료되었습니다!");
      const [data, rs] = await Promise.all([
        classroomApi.getSchedules(from, to),
        memberApi.getReservations(),
      ]);
      setSchedules(data);
      setMyReservations(rs);
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "예약 실패");
    } finally {
      setReserving(null);
    }
  };

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white pb-20">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-gradient-to-r from-white to-[#FFF5F7] px-6 py-4 flex items-center gap-3 border-b border-[var(--color-border)]">
        <button onClick={() => router.push("/home")} className="text-[var(--color-text-title)]">
          <ChevronLeft className="h-6 w-6" />
        </button>
        <h1 className="flex-1 text-[20px] font-bold text-[var(--color-text-title)]">수업 예약</h1>
        <Link
          href="/timetable"
          aria-label="주간 시간표 보기"
          className="inline-flex items-center gap-1 rounded-full border border-[#F0EBE8] bg-white px-2.5 py-1.5 text-[12px] font-medium text-[#6B6B6B] no-underline hover:border-[#FAD4DE] hover:text-[#D88A9E]"
        >
          <CalendarDays className="h-4 w-4" />
          시간표
        </Link>
      </header>

      {/* 날짜 선택 */}
      <div className="flex gap-2 overflow-x-auto px-6 py-4 scrollbar-hide">
        {dates.map((d) => {
          const dateStr = format(d, "yyyy-MM-dd");
          const isSelected = dateStr === selectedDateStr;
          return (
            <button
              key={dateStr}
              onClick={() => setSelectedDate(d)}
              className={`flex flex-col items-center min-w-[56px] rounded-[18px] py-2.5 px-3 text-center border transition-all ${
                isSelected
                  ? "bg-[var(--color-pilates)] border-[var(--color-pilates)] text-[var(--color-text-title)] card-elevated"
                  : "bg-white border-[var(--color-border)] text-[var(--color-text-body)]"
              }`}
            >
              <span className="text-[13px]">{format(d, "M/d")}</span>
              <span className="text-[15px] font-semibold">{format(d, "yyyy-MM-dd") === format(new Date(), "yyyy-MM-dd") ? "오늘" : format(d, "EEE", { locale: ko })}</span>
            </button>
          );
        })}
      </div>

      {/* 수업 안내 */}
      <div className="px-6 pt-2">
        <PageGuide text="원하는 날짜를 선택하고 '예약' 버튼을 눌러 수업을 예약하세요. 수강권 1회가 차감됩니다. 마감된 수업은 예약할 수 없습니다." />
      </div>

      {/* 수업 목록 */}
      <div className="px-6 py-2 flex flex-col gap-3">
        {loading ? (
          <div className="text-center py-8 text-[var(--color-text-sub)]">로딩 중...</div>
        ) : daySchedules.length === 0 ? (
          <div className="text-center py-16 text-[var(--color-text-sub)]">
            이 날에는 수업이 없습니다
          </div>
        ) : (
          daySchedules.map((cs) => {
            const isFull = cs.currentCount >= cs.maxCapacity;
            const remaining = cs.maxCapacity - cs.currentCount;
            const classStart = new Date(`${cs.classDate}T${cs.startTime}`);
            const isPast = classStart <= now;
            const isBooked = myReservations.some(r => r.classScheduleId === cs.id && r.status === "CONFIRMED");
            return (
              <div key={cs.id} className={`rounded-[18px] border border-[var(--color-border)] overflow-hidden flex card-elevated card-hover ${isToday && new Date(`${cs.classDate}T${cs.endTime}`) <= now ? "opacity-50" : ""}`}>
                <div className={`w-1 self-stretch rounded-l-[18px] ${lessonColor[cs.lessonTypeName] || "bg-pilates"}`} />
                <div className="flex-1 p-4 flex items-center justify-between">
                  <div className="flex-1">
                    <p className="text-[15px] font-semibold text-[var(--color-text-title)]">
                      {formatTime(cs.startTime)}~{formatTime(cs.endTime)} {cs.lessonTypeName}
                    </p>
                    <p className="text-[12px] text-[var(--color-text-sub)]">{lessonDesc[cs.lessonTypeName] || ""}</p>
                    <div className="flex gap-3 mt-1.5 text-[13px] text-[var(--color-text-body)]">
                      <span className="flex items-center gap-1">
                        <User className="h-3.5 w-3.5" />{cs.instructorName}
                      </span>
                      <span className={`flex items-center gap-1 ${remaining > 0 && remaining <= 2 ? "text-red-500 font-semibold" : ""}`}>
                        <Clock className="h-3.5 w-3.5" />{remaining === 0 ? "마감" : `잔여 ${remaining}석`}
                      </span>
                    </div>
                  </div>
                  {!hasActiveMembership ? (
                    <Link
                      href="/membership"
                      className="rounded-[8px] px-3 py-2 text-[12px] font-semibold bg-[var(--color-bg-section)] text-[var(--color-text-sub)] transition-all"
                    >
                      수강권 구매 필요
                    </Link>
                  ) : isBooked ? (
                    <div className="flex flex-col items-end">
                      <span className="rounded-[8px] px-4 py-2.5 text-[13px] font-semibold bg-green-50 text-green-600 border border-green-200">
                        예약완료
                      </span>
                    </div>
                  ) : (
                    <div className="flex flex-col items-end">
                      <button
                        onClick={() => setConfirmClassId(cs.id)}
                        disabled={isFull || isPast || reserving === cs.id}
                        className={`rounded-[8px] px-4 py-2.5 text-[13px] font-semibold transition-all ${
                          isFull || isPast
                            ? "bg-[var(--color-bg-section)] text-[var(--color-text-sub)] cursor-not-allowed"
                            : "bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] text-[var(--color-text-title)]"
                        }`}
                      >
                        {reserving === cs.id ? "..." : isPast ? "종료" : isFull ? "마감" : "예약"}
                      </button>
                      {isFull && (
                        <p className="text-[11px] text-[var(--color-text-sub)] mt-1">다른 시간을 확인해보세요</p>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {confirmClassId && (() => {
        const cs = daySchedules.find(s => s.id === confirmClassId);
        if (!cs) return null;
        return (
          <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={() => setConfirmClassId(null)}>
            <div className="bg-white rounded-[18px] max-w-[360px] w-full p-6 card-elevated-lg" onClick={e => e.stopPropagation()}>
              <h3 className="text-[18px] font-bold text-[var(--color-text-title)] mb-4">예약 확인</h3>
              <div className="rounded-[12px] bg-[var(--color-bg-section)] p-4 mb-4">
                <p className="text-[15px] font-semibold text-[var(--color-text-title)]">{formatTime(cs.startTime)}~{formatTime(cs.endTime)} {cs.lessonTypeName}</p>
                <p className="text-[13px] text-[var(--color-text-body)] mt-1">{cs.instructorName} 선생님</p>
              </div>
              <p className="text-[13px] text-[var(--color-text-body)] mb-5 text-center">수강권 1회가 차감됩니다</p>
              <div className="flex gap-3">
                <button onClick={() => setConfirmClassId(null)} className="flex-1 rounded-[8px] border border-[var(--color-border)] py-3 text-[15px] font-semibold text-[var(--color-text-body)]">취소</button>
                <button onClick={() => { handleReserve(confirmClassId); setConfirmClassId(null); }} disabled={reserving === confirmClassId} className="flex-1 rounded-[8px] bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] py-3 text-[15px] font-semibold text-[var(--color-text-title)] disabled:opacity-60">{reserving === confirmClassId ? "예약 중..." : "예약하기"}</button>
              </div>
            </div>
          </div>
        );
      })()}

      <MobileTabBar />
    </div>
  );
}
