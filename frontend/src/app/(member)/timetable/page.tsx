"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { addDays, format, startOfWeek } from "date-fns";
import { ko } from "date-fns/locale";
import { ChevronLeft, ChevronRight, List } from "lucide-react";
import Link from "next/link";
import { classroomApi } from "@/lib/api/classroom";
import { memberApi } from "@/lib/api/member";
import { reservationApi } from "@/lib/api/reservation";
import type {
  ClassSchedule,
  Membership,
  Reservation,
} from "@/lib/types/domain";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { formatTime } from "@/lib/utils/format";

const HOUR_START = 7;
const HOUR_END = 22;
const HOURS = Array.from({ length: HOUR_END - HOUR_START + 1 }, (_, i) => HOUR_START + i);
const SLOT_HEIGHT = 56;

const LESSON_PALETTE: Record<string, { bg: string; border: string; text: string; chip: string }> = {
  개인: {
    bg: "linear-gradient(180deg,#EAF1FF 0%,#D7E3FE 100%)",
    border: "#A4BDF5",
    text: "#1F3A8A",
    chip: "#4F6FCB",
  },
  듀엣: {
    bg: "linear-gradient(180deg,#F3EAFE 0%,#E2D0FB 100%)",
    border: "#BFA4EE",
    text: "#4C2D91",
    chip: "#7E57C2",
  },
  그룹: {
    bg: "linear-gradient(180deg,#E8F6EC 0%,#D2EBD8 100%)",
    border: "#A6D4B0",
    text: "#1F6B33",
    chip: "#3F9156",
  },
  체험: {
    bg: "linear-gradient(180deg,#FFF3DE 0%,#FFE2B3 100%)",
    border: "#F0C77A",
    text: "#7A4A0E",
    chip: "#C68A2E",
  },
};

const DEFAULT_PALETTE = {
  bg: "linear-gradient(180deg,#FFEDF3 0%,#FAD4DE 100%)",
  border: "#F0A0B5",
  text: "#88324A",
  chip: "#D88A9E",
};

function paletteFor(name: string) {
  return LESSON_PALETTE[name] ?? DEFAULT_PALETTE;
}

function toMinutes(hhmmss: string) {
  const [h, m] = hhmmss.split(":").map(Number);
  return h * 60 + m;
}

export default function TimetablePage() {
  const router = useRouter();
  const [schedules, setSchedules] = useState<ClassSchedule[]>([]);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [myReservations, setMyReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [reserving, setReserving] = useState<number | null>(null);
  const [selected, setSelected] = useState<ClassSchedule | null>(null);

  const [weekStart, setWeekStart] = useState(() =>
    startOfWeek(new Date(), { weekStartsOn: 1 }),
  );
  const weekDates = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)),
    [weekStart],
  );
  const from = format(weekDates[0], "yyyy-MM-dd");
  const to = format(weekDates[6], "yyyy-MM-dd");

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const [data, ms, rs] = await Promise.all([
          classroomApi.getSchedules(from, to),
          memberApi.getMemberships(),
          memberApi.getReservations(),
        ]);
        if (cancelled) return;
        setSchedules(data);
        setMemberships(ms);
        setMyReservations(rs);
      } catch {
        // empty
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [from, to]);

  const hasActiveMembership = memberships.some((m) => m.status === "ACTIVE");
  const todayStr = format(new Date(), "yyyy-MM-dd");
  const isCurrentWeek = weekDates.some((d) => format(d, "yyyy-MM-dd") === todayStr);

  const reservedClassIds = new Set(
    myReservations
      .filter((r) => r.status === "CONFIRMED")
      .map((r) => r.classScheduleId),
  );

  const byDate = useMemo(() => {
    const m = new Map<string, ClassSchedule[]>();
    for (const s of schedules) {
      const arr = m.get(s.classDate) ?? [];
      arr.push(s);
      m.set(s.classDate, arr);
    }
    return m;
  }, [schedules]);

  const handleReserve = async (id: number) => {
    setReserving(id);
    try {
      await reservationApi.create(id);
      toast.success("예약이 완료되었습니다!");
      const [data, rs] = await Promise.all([
        classroomApi.getSchedules(from, to),
        memberApi.getReservations(),
      ]);
      setSchedules(data);
      setMyReservations(rs);
      setSelected(null);
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "예약 실패");
    } finally {
      setReserving(null);
    }
  };

  const goPrev = () => setWeekStart((d) => addDays(d, -7));
  const goNext = () => setWeekStart((d) => addDays(d, 7));
  const goToday = () =>
    setWeekStart(startOfWeek(new Date(), { weekStartsOn: 1 }));

  const headerLabel = `${format(weekDates[0], "yyyy.MM.dd")} - ${format(weekDates[6], "MM.dd")}`;

  return (
    <div className="relative mx-auto min-h-screen max-w-[560px] bg-white pb-24">
      {/* Header */}
      <header className="sticky top-0 z-20 border-b border-[#F0EBE8] bg-white/95 px-5 pt-4 pb-3 backdrop-blur">
        <div className="mb-2 flex items-center justify-between">
          <button
            onClick={() => router.back()}
            aria-label="뒤로"
            className="-ml-2 flex h-9 w-9 items-center justify-center rounded-full text-[#2A2A2C] hover:bg-[#F7F2EE]"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>
          <h1 className="text-[18px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
            주간 시간표
          </h1>
          <Link
            href="/schedule"
            aria-label="리스트 보기"
            className="flex h-9 w-9 items-center justify-center rounded-full text-[#6B6B6B] no-underline hover:bg-[#F7F2EE]"
          >
            <List className="h-5 w-5" />
          </Link>
        </div>

        {/* Week navigator */}
        <div className="flex items-center justify-between">
          <button
            onClick={goPrev}
            aria-label="이전 주"
            className="flex h-8 w-8 items-center justify-center rounded-full border border-[#F0EBE8] bg-white text-[#6B6B6B] hover:border-[#FAD4DE]"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>

          <div className="flex flex-col items-center">
            <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#D88A9E]">
              {isCurrentWeek ? "THIS WEEK" : "WEEK"}
            </span>
            <span className="text-[14px] font-semibold tracking-[-0.01em] text-[#2A2A2C]">
              {headerLabel}
            </span>
            {!isCurrentWeek && (
              <button
                onClick={goToday}
                className="mt-0.5 text-[11px] font-medium text-[#D88A9E] underline-offset-2 hover:underline"
              >
                오늘로 이동
              </button>
            )}
          </div>

          <button
            onClick={goNext}
            aria-label="다음 주"
            className="flex h-8 w-8 items-center justify-center rounded-full border border-[#F0EBE8] bg-white text-[#6B6B6B] hover:border-[#FAD4DE]"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </header>

      {/* Day column headers */}
      <div className="sticky top-[110px] z-10 grid grid-cols-[44px_repeat(7,minmax(0,1fr))] border-b border-[#F0EBE8] bg-white">
        <div />
        {weekDates.map((d) => {
          const ds = format(d, "yyyy-MM-dd");
          const isToday = ds === todayStr;
          const dow = format(d, "EEE", { locale: ko });
          return (
            <div
              key={ds}
              className={`flex flex-col items-center py-2 text-center ${
                isToday ? "text-[#D88A9E]" : "text-[#6B6B6B]"
              }`}
            >
              <span className="text-[10px] font-semibold tracking-[0.04em]">
                {dow}
              </span>
              <span
                className={`mt-0.5 flex h-6 w-6 items-center justify-center rounded-full text-[12px] font-bold ${
                  isToday ? "bg-[#F0A0B5] text-white" : "text-[#2A2A2C]"
                }`}
              >
                {format(d, "d")}
              </span>
            </div>
          );
        })}
      </div>

      {/* Grid body */}
      <div className="relative px-0">
        {loading ? (
          <div className="py-16 text-center text-[13px] text-[#A0A0A0]">
            시간표를 불러오는 중...
          </div>
        ) : (
          <div className="relative grid grid-cols-[44px_repeat(7,minmax(0,1fr))]">
            {/* Hour gutter */}
            <div className="relative">
              {HOURS.map((h) => (
                <div
                  key={h}
                  className="flex items-start justify-end pr-1.5 pt-[2px] text-[10px] font-medium text-[#A0A0A0]"
                  style={{ height: SLOT_HEIGHT }}
                >
                  {String(h).padStart(2, "0")}
                </div>
              ))}
            </div>

            {/* Day columns */}
            {weekDates.map((d) => {
              const ds = format(d, "yyyy-MM-dd");
              const isToday = ds === todayStr;
              const daySchedules = (byDate.get(ds) ?? []).filter(
                (s) => s.status !== "CANCELLED",
              );
              return (
                <div
                  key={ds}
                  className={`relative border-l border-[#F4EFEC] ${
                    isToday ? "bg-[#FFFAFB]" : ""
                  }`}
                >
                  {/* Hour grid lines */}
                  {HOURS.map((h) => (
                    <div
                      key={h}
                      className="border-b border-[#F4EFEC]"
                      style={{ height: SLOT_HEIGHT }}
                    />
                  ))}

                  {/* Class blocks */}
                  {daySchedules.map((cs) => {
                    const startMin = toMinutes(cs.startTime);
                    const endMin = toMinutes(cs.endTime);
                    const top =
                      ((startMin - HOUR_START * 60) / 60) * SLOT_HEIGHT;
                    const height = Math.max(
                      28,
                      ((endMin - startMin) / 60) * SLOT_HEIGHT - 2,
                    );
                    if (top < -SLOT_HEIGHT || top > HOURS.length * SLOT_HEIGHT)
                      return null;
                    const p = paletteFor(cs.lessonTypeName);
                    const isFull = cs.currentCount >= cs.maxCapacity;
                    const isBooked = reservedClassIds.has(cs.id);
                    return (
                      <button
                        key={cs.id}
                        onClick={() => setSelected(cs)}
                        className="absolute left-[2px] right-[2px] overflow-hidden rounded-[6px] border px-1 py-[3px] text-left transition-transform active:scale-[0.98]"
                        style={{
                          top,
                          height,
                          background: p.bg,
                          borderColor: isBooked ? "#3F9156" : p.border,
                          color: p.text,
                          boxShadow: isBooked
                            ? "0 0 0 1.5px #3F9156 inset"
                            : undefined,
                          opacity: isFull && !isBooked ? 0.55 : 1,
                        }}
                        title={`${formatTime(cs.startTime)}~${formatTime(cs.endTime)} ${cs.lessonTypeName} · ${cs.instructorName}`}
                      >
                        <div className="flex items-center gap-[3px] text-[9px] font-bold leading-none">
                          <span
                            className="inline-block h-[5px] w-[5px] rounded-full"
                            style={{ background: p.chip }}
                          />
                          {formatTime(cs.startTime)}
                        </div>
                        <div className="mt-[2px] truncate text-[10px] font-semibold leading-tight">
                          {cs.lessonTypeName}
                        </div>
                        {height > 38 && (
                          <div className="mt-[1px] truncate text-[9px] font-medium opacity-80">
                            {cs.instructorName}
                          </div>
                        )}
                        {isBooked && (
                          <div className="absolute right-[2px] top-[2px] rounded-sm bg-[#3F9156] px-[3px] py-[1px] text-[8px] font-bold leading-none text-white">
                            예약
                          </div>
                        )}
                        {isFull && !isBooked && (
                          <div className="absolute right-[2px] top-[2px] rounded-sm bg-[#888] px-[3px] py-[1px] text-[8px] font-bold leading-none text-white">
                            마감
                          </div>
                        )}
                      </button>
                    );
                  })}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Legend */}
      <div className="mt-3 flex flex-wrap items-center justify-center gap-x-3 gap-y-1.5 px-5 text-[11px] text-[#6B6B6B]">
        {Object.entries(LESSON_PALETTE).map(([name, p]) => (
          <span key={name} className="inline-flex items-center gap-1">
            <span
              className="inline-block h-2 w-2 rounded-full"
              style={{ background: p.chip }}
            />
            {name}
          </span>
        ))}
        <span className="inline-flex items-center gap-1">
          <span className="inline-block h-2 w-2 rounded-sm border-[1.5px] border-[#3F9156]" />
          내 예약
        </span>
      </div>

      <p className="mt-2 px-5 text-center text-[11px] text-[#A0A0A0]">
        수업 블록을 누르면 상세 정보와 예약을 진행할 수 있어요
      </p>

      {/* Detail / reserve modal */}
      {selected && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 sm:items-center"
          onClick={() => setSelected(null)}
        >
          <div
            className="w-full max-w-[420px] rounded-t-[20px] bg-white p-6 pb-7 sm:rounded-[20px]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4 flex items-start justify-between">
              <div>
                <div className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#D88A9E]">
                  {format(new Date(selected.classDate), "M월 d일 (EEE)", {
                    locale: ko,
                  })}
                </div>
                <div className="mt-1 text-[20px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
                  {formatTime(selected.startTime)}~{formatTime(selected.endTime)}{" "}
                  {selected.lessonTypeName}
                </div>
                <div className="mt-1 text-[13px] text-[#6B6B6B]">
                  {selected.instructorName} 강사
                </div>
              </div>
              <div
                className="flex h-8 items-center rounded-full border px-2.5 text-[11px] font-semibold"
                style={{
                  borderColor: paletteFor(selected.lessonTypeName).chip,
                  color: paletteFor(selected.lessonTypeName).chip,
                }}
              >
                {selected.maxCapacity - selected.currentCount > 0
                  ? `잔여 ${selected.maxCapacity - selected.currentCount}석`
                  : "마감"}
              </div>
            </div>

            {(() => {
              const isFull = selected.currentCount >= selected.maxCapacity;
              const isBooked = reservedClassIds.has(selected.id);
              const isPast =
                new Date(`${selected.classDate}T${selected.startTime}`) <=
                new Date();
              if (!hasActiveMembership) {
                return (
                  <Link
                    href="/membership"
                    className="block w-full rounded-[10px] bg-[#F7F2EE] py-3 text-center text-[14px] font-semibold text-[#6B6B6B] no-underline"
                  >
                    수강권 구매 필요
                  </Link>
                );
              }
              if (isBooked) {
                return (
                  <div className="rounded-[10px] border border-green-200 bg-green-50 py-3 text-center text-[14px] font-semibold text-green-600">
                    이미 예약된 수업입니다
                  </div>
                );
              }
              return (
                <div className="flex gap-2.5">
                  <button
                    onClick={() => setSelected(null)}
                    className="flex-1 rounded-[10px] border border-[#F0EBE8] py-3 text-[14px] font-semibold text-[#6B6B6B]"
                  >
                    닫기
                  </button>
                  <button
                    disabled={isFull || isPast || reserving === selected.id}
                    onClick={() => handleReserve(selected.id)}
                    className="flex-1 rounded-[10px] py-3 text-[14px] font-semibold text-white disabled:opacity-50"
                    style={{
                      background:
                        "linear-gradient(135deg,#F0A0B5 0%,#D88A9E 100%)",
                    }}
                  >
                    {reserving === selected.id
                      ? "예약 중..."
                      : isPast
                        ? "종료된 수업"
                        : isFull
                          ? "마감"
                          : "예약하기"}
                  </button>
                </div>
              );
            })()}
          </div>
        </div>
      )}

      <MobileTabBar />
    </div>
  );
}
