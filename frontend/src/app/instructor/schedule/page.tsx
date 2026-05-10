"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { format, addDays } from "date-fns";
import { useAuth } from "@/lib/hooks/use-auth";
import { api } from "@/lib/api/client";
import type { ClassSchedule } from "@/lib/types/domain";
import { formatTime } from "@/lib/utils/format";
import { ChevronDown, ChevronUp } from "lucide-react";

const WEEKDAY_KO = ["일", "월", "화", "수", "목", "금", "토"];
const WEEKDAY_EN = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];

const TONE = {
  primary: "#7C8FD4",
  primaryDark: "#5F75C4",
  primaryDeep: "#4A5DA8",
  primaryLight: "#C7D0F2",
  primarySoft: "#EEF0FF",
  primaryFaint: "#F6F7FF",
  border: "#E4E7F2",
};

function formatDateChip(dateStr: string) {
  const d = new Date(`${dateStr}T00:00:00`);
  return {
    month: `${d.getMonth() + 1}월`,
    day: String(d.getDate()).padStart(2, "0"),
    week: WEEKDAY_EN[d.getDay()],
  };
}

function todayBadge(dateStr: string) {
  const d = new Date(`${dateStr}T00:00:00`);
  d.setHours(0, 0, 0, 0);
  const t = new Date();
  t.setHours(0, 0, 0, 0);
  const diff = Math.round((d.getTime() - t.getTime()) / 86400000);
  if (diff === 0) return "오늘";
  if (diff === 1) return "내일";
  return null;
}

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
        if (activeTab === 0) {
          from = to = format(today, "yyyy-MM-dd");
        } else if (activeTab === 1) {
          const tmr = addDays(today, 1);
          from = to = format(tmr, "yyyy-MM-dd");
        } else {
          const mondayOffset = -today.getDay() + 1 + weekOffset * 7;
          const weekStart = addDays(today, mondayOffset);
          from = format(weekStart, "yyyy-MM-dd");
          to = format(addDays(weekStart, 6), "yyyy-MM-dd");
        }
        const data = await api<ClassSchedule[]>(
          "get",
          "/api/instructor/class-schedules",
          { from, to },
        );
        setSchedules(
          data.sort(
            (a, b) =>
              a.classDate.localeCompare(b.classDate) ||
              a.startTime.localeCompare(b.startTime),
          ),
        );
      } catch {
        // empty
      } finally {
        setLoading(false);
      }
    }
    setLoading(true);
    load();
  }, [activeTab, weekOffset]);

  const today = new Date();
  const dateText = `${today.getFullYear()}년 ${today.getMonth() + 1}월 ${today.getDate()}일 ${WEEKDAY_KO[today.getDay()]}요일`;
  const instructorName = user?.name || "강사";

  const todayCount = schedules.filter(
    (s) => s.classDate === format(today, "yyyy-MM-dd"),
  ).length;
  const totalReservations = schedules.reduce(
    (sum, s) => sum + s.currentCount,
    0,
  );
  const fullClasses = schedules.filter(
    (s) => s.currentCount >= s.maxCapacity,
  ).length;

  const nowMs = Date.now();

  return (
    <div
      className="relative mx-auto min-h-screen max-w-[480px] overflow-hidden bg-white pb-10"
      style={{ background: "#F6F7FF" }}
    >
      <div className="bg-white">
        {/* ---------- Hero ---------- */}
        <section
          className="relative overflow-hidden px-6 pb-7 pt-[18px]"
          style={{
            background:
              "radial-gradient(120% 80% at 90% -10%, #C7D0F2 0%, rgba(199,208,242,0) 55%), radial-gradient(80% 60% at -10% 30%, #E1E6FA 0%, rgba(225,230,250,0) 60%), linear-gradient(180deg, #F6F7FF 0%, #FFFFFF 100%)",
          }}
        >
          <span
            className="pointer-events-none absolute -right-10 -top-10 h-[180px] w-[180px] rounded-full"
            style={{
              background:
                "radial-gradient(circle, rgba(124,143,212,0.18) 0%, rgba(124,143,212,0) 70%)",
            }}
          />
          {/* Header */}
          <div className="relative z-[2] flex items-center justify-between">
            <div className="flex items-center gap-2 text-[16px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
              <div
                className="flex h-7 w-7 items-center justify-center rounded-[8px] text-[14px] font-extrabold italic text-white"
                style={{
                  background: TONE.primary,
                  fontFamily: "Georgia, serif",
                }}
              >
                T
              </div>
              필라테스 OO점
            </div>
            <button
              onClick={logout}
              className="flex h-9 items-center gap-1 rounded-full border border-[rgba(228,231,242,0.8)] bg-white/70 px-3 text-[12px] font-medium text-[#6B6B6B] backdrop-blur-md transition-colors hover:bg-white"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.8}
                stroke="currentColor"
                className="h-3.5 w-3.5"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15M12 9l-3 3m0 0 3 3m-3-3h12.75"
                />
              </svg>
              로그아웃
            </button>
          </div>

          {/* Greeting */}
          <div className="relative z-[2] mt-[26px]">
            <div className="mb-1.5 text-[13px] font-medium tracking-[0.01em] text-[#A0A0A0]">
              {dateText}
            </div>
            <h1 className="text-[26px] font-bold leading-[1.3] tracking-[-0.03em] text-[#2A2A2C]">
              <em
                className="not-italic"
                style={{ color: TONE.primaryDeep }}
              >
                {instructorName}
              </em>{" "}
              강사님,
              <br />
              오늘도 좋은 수업 만들어요.
            </h1>
            <p className="mt-1.5 text-[14px] text-[#6B6B6B]">
              {loading
                ? "수업 정보를 불러오는 중이에요."
                : todayCount > 0
                  ? `오늘 ${todayCount}건의 수업이 예정되어 있어요.`
                  : "오늘은 배정된 수업이 없어요."}
            </p>
          </div>
        </section>
      </div>

      <main className="relative z-[3] -mt-5 flex flex-col gap-6 px-5">
        {/* Tab Toggle */}
        <div
          className="flex gap-0.5 rounded-full bg-white p-[3px]"
          style={{
            border: `1px solid ${TONE.border}`,
            boxShadow: "0 1px 2px rgba(45,30,30,0.04)",
          }}
        >
          {["오늘", "내일", "이번 주"].map((tab, i) => {
            const active = i === activeTab;
            return (
              <button
                key={tab}
                onClick={() => {
                  setActiveTab(i);
                  setWeekOffset(0);
                }}
                className="flex-1 cursor-pointer rounded-full border-none px-3.5 py-2 text-[13px] font-semibold transition-all"
                style={{
                  background: active ? TONE.primary : "transparent",
                  color: active ? "#fff" : "#6B6B6B",
                }}
              >
                {tab}
              </button>
            );
          })}
        </div>

        {/* Stats summary */}
        {!loading && activeTab === 0 && (
          <div className="grid grid-cols-3 gap-2.5">
            <div
              className="rounded-[18px] border bg-white p-3.5 text-center"
              style={{ borderColor: TONE.border }}
            >
              <p className="text-[11px] text-[#A0A0A0]">오늘 수업</p>
              <p
                className="mt-1 text-[22px] font-extrabold tracking-[-0.03em] text-[#2A2A2C]"
                style={{}}
              >
                {schedules.length}
              </p>
            </div>
            <div
              className="rounded-[18px] border bg-white p-3.5 text-center"
              style={{ borderColor: TONE.border }}
            >
              <p className="text-[11px] text-[#A0A0A0]">총 수강생</p>
              <p
                className="mt-1 text-[22px] font-extrabold tracking-[-0.03em]"
                style={{ color: TONE.primaryDark }}
              >
                {totalReservations}
              </p>
            </div>
            <div
              className="rounded-[18px] border bg-white p-3.5 text-center"
              style={{ borderColor: TONE.border }}
            >
              <p className="text-[11px] text-[#A0A0A0]">마감</p>
              <p className="mt-1 text-[22px] font-extrabold tracking-[-0.03em] text-[#E76F51]">
                {fullClasses}
              </p>
            </div>
          </div>
        )}

        {/* Week summary chart */}
        {!loading && activeTab === 2 && schedules.length > 0 && (
          <div
            className="rounded-[18px] border bg-white p-4"
            style={{ borderColor: TONE.border }}
          >
            <div className="mb-3 flex items-center justify-between">
              <div>
                <span
                  className="mb-0.5 block text-[10px] font-semibold uppercase tracking-[0.06em]"
                  style={{ color: TONE.primaryDark }}
                >
                  THIS WEEK
                </span>
                <h3 className="text-[14px] font-bold text-[#2A2A2C]">
                  요일별 수업 배정
                </h3>
              </div>
              <div className="flex items-center gap-2 text-[12px]">
                <button
                  onClick={() => setWeekOffset(weekOffset - 1)}
                  className="flex h-7 w-7 items-center justify-center rounded-full border bg-white text-[#6B6B6B] hover:border-[#7C8FD4] hover:text-[#5F75C4]"
                  style={{ borderColor: TONE.border }}
                >
                  ←
                </button>
                {weekOffset !== 0 && (
                  <button
                    onClick={() => setWeekOffset(0)}
                    className="rounded-full px-2.5 py-1 text-[11px] font-semibold"
                    style={{
                      background: TONE.primarySoft,
                      color: TONE.primaryDeep,
                    }}
                  >
                    이번 주
                  </button>
                )}
                <button
                  onClick={() => setWeekOffset(weekOffset + 1)}
                  className="flex h-7 w-7 items-center justify-center rounded-full border bg-white text-[#6B6B6B] hover:border-[#7C8FD4] hover:text-[#5F75C4]"
                  style={{ borderColor: TONE.border }}
                >
                  →
                </button>
              </div>
            </div>
            <div className="flex justify-between text-center">
              {["월", "화", "수", "목", "금", "토", "일"].map((day, i) => {
                const targetDay = i === 6 ? 0 : i + 1;
                const dayCount = schedules.filter(
                  (s) =>
                    new Date(s.classDate + "T00:00:00").getDay() === targetDay,
                ).length;
                const has = dayCount > 0;
                return (
                  <div key={day} className="flex flex-1 flex-col items-center gap-1.5">
                    <span className="text-[11px] text-[#A0A0A0]">{day}</span>
                    <span
                      className={`flex h-8 w-8 items-center justify-center rounded-full text-[14px] font-bold transition-all ${has ? "" : "border"}`}
                      style={
                        has
                          ? {
                              background: TONE.primary,
                              color: "#fff",
                            }
                          : {
                              borderColor: TONE.border,
                              color: "#A0A0A0",
                            }
                      }
                    >
                      {dayCount}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Section title */}
        <section>
          <div className="mb-3">
            <span
              className="mb-1 block text-[11px] font-semibold uppercase tracking-[0.06em]"
              style={{ color: TONE.primaryDark }}
            >
              {activeTab === 0
                ? "TODAY"
                : activeTab === 1
                  ? "TOMORROW"
                  : "THIS WEEK"}
            </span>
            <h2 className="text-[18px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
              수업 일정
            </h2>
          </div>

          {loading ? (
            <div className="rounded-[18px] border bg-white py-12 text-center text-[13px] text-[#A0A0A0]" style={{ borderColor: TONE.border }}>
              불러오는 중...
            </div>
          ) : schedules.length === 0 ? (
            <div
              className="rounded-[18px] border bg-white p-8 text-center"
              style={{ borderColor: TONE.border }}
            >
              <div className="mb-3 text-[40px]">☕</div>
              <p className="mb-1 text-[15px] font-semibold text-[#2A2A2C]">
                {activeTab === 0
                  ? "오늘 배정된 수업이 없어요"
                  : activeTab === 1
                    ? "내일 배정된 수업이 없어요"
                    : "이번 주 배정된 수업이 없어요"}
              </p>
              <p className="text-[13px] text-[#6B6B6B]">
                수업 배정은 관리자 시간표에서 설정합니다.
              </p>
            </div>
          ) : (
            <div className="flex flex-col gap-2.5">
              {schedules.map((cs, idx) => {
                const showDateHeader =
                  activeTab === 2 &&
                  (idx === 0 || schedules[idx - 1].classDate !== cs.classDate);
                const chip = formatDateChip(cs.classDate);
                const tBadge = todayBadge(cs.classDate);
                const startMs = new Date(
                  `${cs.classDate}T${cs.startTime}`,
                ).getTime();
                const endMs = new Date(
                  `${cs.classDate}T${cs.endTime}`,
                ).getTime();
                const isOngoing = startMs <= nowMs && nowMs < endMs;
                const remaining = cs.maxCapacity - cs.currentCount;
                const isFull = remaining <= 0;
                const isExpanded = expandedId === cs.id;
                return (
                  <div key={cs.id}>
                    {showDateHeader && (
                      <div className="mb-2 mt-3 first:mt-0">
                        <div
                          className="text-[11px] font-semibold uppercase tracking-[0.06em]"
                          style={{ color: TONE.primaryDark }}
                        >
                          {chip.month} {chip.day} · {chip.week}
                        </div>
                      </div>
                    )}
                    <div
                      className={`rounded-[18px] border bg-white transition-all ${isOngoing ? "" : ""}`}
                      style={{
                        borderColor: isOngoing
                          ? TONE.primaryLight
                          : TONE.border,
                        background: isOngoing
                          ? "linear-gradient(180deg, #F6F7FF 0%, #FFFFFF 100%)"
                          : "#FFFFFF",
                      }}
                    >
                      <button
                        onClick={() =>
                          setExpandedId(isExpanded ? null : cs.id)
                        }
                        className="flex w-full cursor-pointer items-center gap-3.5 border-none bg-transparent p-3.5 text-left"
                      >
                        <div
                          className="flex h-16 w-14 flex-shrink-0 flex-col items-center justify-center rounded-[14px]"
                          style={{
                            background: isOngoing
                              ? TONE.primary
                              : TONE.primarySoft,
                            border: `1px solid ${isOngoing ? TONE.primary : "rgba(124,143,212,0.15)"}`,
                          }}
                        >
                          <span
                            className={`text-[10px] font-semibold tracking-[0.04em]`}
                            style={{
                              color: isOngoing
                                ? "rgba(255,255,255,0.95)"
                                : TONE.primaryDark,
                            }}
                          >
                            {chip.month}
                          </span>
                          <span
                            className={`mt-0.5 text-[22px] font-extrabold leading-none tracking-[-0.03em]`}
                            style={{
                              color: isOngoing ? "#fff" : "#2A2A2C",
                            }}
                          >
                            {chip.day}
                          </span>
                          <span
                            className={`mt-0.5 text-[10px]`}
                            style={{
                              color: isOngoing
                                ? "rgba(255,255,255,0.85)"
                                : "#A0A0A0",
                            }}
                          >
                            {chip.week}
                          </span>
                        </div>

                        <div className="min-w-0 flex-1">
                          <div className="mb-1 flex items-center gap-1.5">
                            <span
                              className="text-[12px] font-semibold"
                              style={{ color: TONE.primaryDark }}
                            >
                              {formatTime(cs.startTime)} - {formatTime(cs.endTime)}
                            </span>
                            {isOngoing && (
                              <span
                                className="rounded-full px-1.5 py-px text-[9px] font-bold tracking-[0.04em] text-white"
                                style={{ background: "#22A259" }}
                              >
                                ● 진행 중
                              </span>
                            )}
                            {tBadge && !isOngoing && activeTab !== 0 && (
                              <span
                                className="rounded-full border px-1.5 py-px text-[9px] font-bold tracking-[0.04em]"
                                style={{
                                  borderColor: TONE.primaryLight,
                                  color: TONE.primaryDark,
                                  background: "#fff",
                                }}
                              >
                                {tBadge}
                              </span>
                            )}
                          </div>
                          <div className="mb-[3px] text-[15px] font-semibold tracking-[-0.01em] text-[#2A2A2C]">
                            {cs.lessonTypeName}
                          </div>
                          <div className="flex items-center gap-1.5 text-[12px] text-[#6B6B6B]">
                            <span>
                              {cs.currentCount}명 예약
                              {!isFull && ` · 잔여 ${remaining}석`}
                              {isFull && (
                                <span className="ml-1 font-semibold text-[#E76F51]">
                                  · 마감
                                </span>
                              )}
                            </span>
                          </div>
                        </div>
                        <span
                          className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full"
                          style={{ background: TONE.primaryFaint }}
                        >
                          {isExpanded ? (
                            <ChevronUp
                              className="h-4 w-4"
                              style={{ color: TONE.primaryDark }}
                            />
                          ) : (
                            <ChevronDown
                              className="h-4 w-4"
                              style={{ color: TONE.primaryDark }}
                            />
                          )}
                        </span>
                      </button>

                      {isExpanded && (
                        <div
                          className="border-t px-3.5 pb-3.5 pt-3"
                          style={{ borderColor: TONE.border }}
                        >
                          <div className="mb-3 grid grid-cols-2 gap-3 text-[13px]">
                            <div>
                              <p className="text-[11px] text-[#A0A0A0]">유형</p>
                              <p className="font-semibold text-[#2A2A2C]">
                                {cs.lessonTypeName}
                              </p>
                            </div>
                            <div>
                              <p className="text-[11px] text-[#A0A0A0]">
                                수업 시간
                              </p>
                              <p className="font-semibold text-[#2A2A2C]">
                                50분
                              </p>
                            </div>
                            <div>
                              <p className="text-[11px] text-[#A0A0A0]">
                                예약 현황
                              </p>
                              <p className="font-semibold text-[#2A2A2C]">
                                {cs.currentCount}명 / {cs.maxCapacity}명
                              </p>
                            </div>
                            <div>
                              <p className="text-[11px] text-[#A0A0A0]">잔여</p>
                              <p
                                className={`font-semibold ${
                                  remaining <= 2 && remaining > 0
                                    ? "text-[#E76F51]"
                                    : "text-[#2A2A2C]"
                                }`}
                              >
                                {isFull ? "마감" : `${remaining}석`}
                              </p>
                            </div>
                          </div>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              router.push(
                                `/instructor/attendance?classId=${cs.id}`,
                              );
                            }}
                            disabled={cs.currentCount === 0}
                            className="w-full cursor-pointer rounded-[12px] border-none px-4 py-3 text-[14px] font-semibold text-white transition-[transform,box-shadow] disabled:cursor-not-allowed disabled:opacity-50"
                            style={
                              cs.currentCount === 0
                                ? {
                                    background: "#E0E2EC",
                                    color: "#A0A0A0",
                                  }
                                : {
                                    background:
                                      "linear-gradient(135deg, #8FA1DC 0%, #7C8FD4 60%, #5F75C4 100%)",
                                    boxShadow:
                                      "0 4px 12px -4px rgba(95, 117, 196, 0.45)",
                                  }
                            }
                          >
                            {cs.currentCount === 0
                              ? "예약 없음"
                              : "출석 체크 →"}
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
