"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { memberApi } from "@/lib/api/member";
import type { Reservation } from "@/lib/types/domain";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { usePageTitle } from "@/lib/hooks/use-page-title";

const WEEKDAY_KO = ["일", "월", "화", "수", "목", "금", "토"];

type AttendanceKind = "ATTENDED" | "NO_SHOW" | "CANCELLED" | "UPCOMING";

interface AttendanceRow {
  reservation: Reservation;
  kind: AttendanceKind;
  monthKey: string; // YYYY-MM
  monthLabel: string;
}

function classifyReservation(r: Reservation, todayStr: string): AttendanceKind {
  if (r.status === "NO_SHOW") return "NO_SHOW";
  if (r.status === "CANCELLED") return "CANCELLED";
  if (r.classDate >= todayStr) return "UPCOMING";
  return "ATTENDED";
}

const KIND_META: Record<
  AttendanceKind,
  { label: string; bg: string; color: string; dot: string }
> = {
  ATTENDED: {
    label: "출석",
    bg: "rgba(76, 175, 80, 0.10)",
    color: "#22A259",
    dot: "#22A259",
  },
  NO_SHOW: {
    label: "노쇼",
    bg: "rgba(231, 111, 81, 0.10)",
    color: "#E76F51",
    dot: "#E76F51",
  },
  CANCELLED: {
    label: "취소",
    bg: "rgba(160, 160, 160, 0.12)",
    color: "#6B6B6B",
    dot: "#A0A0A0",
  },
  UPCOMING: {
    label: "예정",
    bg: "#FDEDF2",
    color: "#D88A9E",
    dot: "#D88A9E",
  },
};

function getMonthLabel(dateStr: string) {
  const d = new Date(`${dateStr}T00:00:00`);
  const now = new Date();
  const isCurrentMonth =
    d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
  return {
    key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`,
    label: `${d.getFullYear()}년 ${d.getMonth() + 1}월${isCurrentMonth ? " · 이번 달" : ""}`,
  };
}

export default function MemberAttendancePage() {
  usePageTitle("출석 현황");
  const router = useRouter();
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeFilter, setActiveFilter] = useState<"ALL" | AttendanceKind>(
    "ALL",
  );

  useEffect(() => {
    async function load() {
      try {
        const rs = await memberApi.getReservations();
        setReservations(rs);
      } catch {
        // empty
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const todayStr = new Date().toISOString().split("T")[0];

  const allRows: AttendanceRow[] = useMemo(() => {
    return reservations
      .map((r) => {
        const kind = classifyReservation(r, todayStr);
        const m = getMonthLabel(r.classDate);
        return {
          reservation: r,
          kind,
          monthKey: m.key,
          monthLabel: m.label,
        };
      })
      .sort(
        (a, b) =>
          b.reservation.classDate.localeCompare(a.reservation.classDate) ||
          b.reservation.startTime.localeCompare(a.reservation.startTime),
      );
  }, [reservations, todayStr]);

  // Stats: 최근 90일 기준
  const ninetyDaysAgo = new Date();
  ninetyDaysAgo.setDate(ninetyDaysAgo.getDate() - 90);
  const ninetyStr = ninetyDaysAgo.toISOString().split("T")[0];

  const recentRows = allRows.filter(
    (r) => r.reservation.classDate >= ninetyStr,
  );
  const recentResolved = recentRows.filter(
    (r) => r.kind === "ATTENDED" || r.kind === "NO_SHOW",
  );
  const recentAttended = recentRows.filter((r) => r.kind === "ATTENDED");
  const recentNoShow = recentRows.filter((r) => r.kind === "NO_SHOW");
  const attendanceRate = recentResolved.length
    ? Math.round((recentAttended.length / recentResolved.length) * 100)
    : 0;

  // 연속 출석 주
  const streakWeeks = (() => {
    const attendedDates = new Set(
      allRows
        .filter((r) => r.kind === "ATTENDED")
        .map((r) => r.reservation.classDate),
    );
    let streak = 0;
    const cursor = new Date();
    cursor.setHours(0, 0, 0, 0);
    // 주의 시작은 월요일
    const dayOfWeek = (cursor.getDay() + 6) % 7;
    cursor.setDate(cursor.getDate() - dayOfWeek);
    for (;;) {
      const weekStart = new Date(cursor);
      let hasAttendance = false;
      for (let i = 0; i < 7; i++) {
        const d = new Date(weekStart);
        d.setDate(d.getDate() + i);
        const dStr = d.toISOString().split("T")[0];
        if (attendedDates.has(dStr)) {
          hasAttendance = true;
          break;
        }
      }
      if (!hasAttendance) break;
      streak += 1;
      cursor.setDate(cursor.getDate() - 7);
      if (streak > 200) break;
    }
    return streak;
  })();

  const filtered = useMemo(() => {
    if (activeFilter === "ALL") return allRows;
    return allRows.filter((r) => r.kind === activeFilter);
  }, [allRows, activeFilter]);

  // Group by month
  const groupedByMonth = useMemo(() => {
    const map = new Map<string, { label: string; rows: AttendanceRow[] }>();
    for (const row of filtered) {
      if (!map.has(row.monthKey)) {
        map.set(row.monthKey, { label: row.monthLabel, rows: [] });
      }
      map.get(row.monthKey)!.rows.push(row);
    }
    return Array.from(map.entries());
  }, [filtered]);

  const filters: { key: "ALL" | AttendanceKind; label: string }[] = [
    { key: "ALL", label: "전체" },
    { key: "ATTENDED", label: "출석" },
    { key: "NO_SHOW", label: "노쇼" },
    { key: "CANCELLED", label: "취소" },
    { key: "UPCOMING", label: "예정" },
  ];

  return (
    <div className="relative mx-auto min-h-screen max-w-[480px] overflow-hidden bg-white pb-24">
      {/* Hero */}
      <section
        className="relative overflow-hidden px-6 pb-7 pt-[18px]"
        style={{
          background:
            "radial-gradient(120% 80% at 90% -10%, #FAD4DE 0%, rgba(250, 212, 222, 0) 55%), radial-gradient(80% 60% at -10% 30%, #FFE7DF 0%, rgba(255, 231, 223, 0) 60%), linear-gradient(180deg, #FFF8F5 0%, #FFFFFF 100%)",
        }}
      >
        <span
          className="pointer-events-none absolute -right-10 -top-10 h-[180px] w-[180px] rounded-full"
          style={{
            background:
              "radial-gradient(circle, rgba(240,160,181,0.18) 0%, rgba(240,160,181,0) 70%)",
          }}
        />
        <div className="relative z-[2] flex items-center gap-2">
          <button
            onClick={() => router.back()}
            aria-label="뒤로가기"
            className="flex h-10 w-10 items-center justify-center rounded-full border border-[rgba(240,235,232,0.6)] bg-white/70 text-[#2A2A2C] backdrop-blur-md transition-colors hover:bg-white"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.8}
              stroke="currentColor"
              className="h-5 w-5"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15.75 19.5 8.25 12l7.5-7.5"
              />
            </svg>
          </button>
          <span className="text-[16px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
            출석 현황
          </span>
        </div>

        <div className="relative z-[2] mt-5">
          <span className="mb-1 block text-[11px] font-semibold uppercase tracking-[0.06em] text-[#D88A9E]">
            ATTENDANCE
          </span>
          <h1 className="text-[24px] font-bold leading-[1.3] tracking-[-0.03em] text-[#2A2A2C]">
            지난 수업 출석 기록을
            <br />한눈에 확인해보세요.
          </h1>
          <p className="mt-1.5 text-[14px] text-[#6B6B6B]">
            {loading
              ? "출석 기록을 불러오는 중이에요."
              : `최근 90일 ${recentResolved.length}건 중 ${recentAttended.length}건 출석`}
          </p>
        </div>
      </section>

      <main className="relative z-[3] -mt-3 flex flex-col gap-5 px-5">
        {/* Stats */}
        <section className="grid grid-cols-3 gap-2.5">
          <div className="rounded-[18px] border border-[#F0EBE8] bg-white p-3.5 text-center">
            <p className="text-[11px] text-[#A0A0A0]">출석률</p>
            <p className="mt-1 text-[22px] font-extrabold tracking-[-0.03em] text-[#D88A9E]">
              {attendanceRate}
              <small className="ml-0.5 text-[12px] font-medium text-[#6B6B6B]">
                %
              </small>
            </p>
            <p className="mt-0.5 text-[10px] text-[#A0A0A0]">최근 90일</p>
          </div>
          <div className="rounded-[18px] border border-[#F0EBE8] bg-white p-3.5 text-center">
            <p className="text-[11px] text-[#A0A0A0]">출석 횟수</p>
            <p className="mt-1 text-[22px] font-extrabold tracking-[-0.03em] text-[#2A2A2C]">
              {recentAttended.length}
              <small className="ml-0.5 text-[12px] font-medium text-[#6B6B6B]">
                회
              </small>
            </p>
            <p className="mt-0.5 text-[10px] text-[#A0A0A0]">최근 90일</p>
          </div>
          <div className="rounded-[18px] border border-[#F0EBE8] bg-white p-3.5 text-center">
            <p className="text-[11px] text-[#A0A0A0]">연속 출석</p>
            <p className="mt-1 text-[22px] font-extrabold tracking-[-0.03em] text-[#2A2A2C]">
              {streakWeeks}
              <small className="ml-0.5 text-[12px] font-medium text-[#6B6B6B]">
                주
              </small>
            </p>
            <p className="mt-0.5 text-[10px] text-[#A0A0A0]">
              {recentNoShow.length > 0 ? `노쇼 ${recentNoShow.length}` : "이상 없음"}
            </p>
          </div>
        </section>

        {/* Filter chips */}
        <div className="flex gap-1.5 overflow-x-auto">
          {filters.map((f) => {
            const active = activeFilter === f.key;
            const count =
              f.key === "ALL"
                ? allRows.length
                : allRows.filter((r) => r.kind === f.key).length;
            return (
              <button
                key={f.key}
                onClick={() => setActiveFilter(f.key)}
                className={`flex-shrink-0 rounded-full border px-3 py-1.5 text-[12px] font-medium transition-colors ${
                  active
                    ? "border-[#F0A0B5] bg-[#F0A0B5] text-white"
                    : "border-[#F0EBE8] bg-white text-[#6B6B6B]"
                }`}
              >
                {f.label}
                <span
                  className={`ml-1 ${
                    active ? "text-white/85" : "text-[#A0A0A0]"
                  }`}
                >
                  {count}
                </span>
              </button>
            );
          })}
        </div>

        {/* List */}
        <section>
          <div className="mb-3">
            <span className="mb-1 block text-[11px] font-semibold uppercase tracking-[0.06em] text-[#D88A9E]">
              HISTORY
            </span>
            <h2 className="text-[18px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
              출석 내역
            </h2>
          </div>

          {loading ? (
            <div className="rounded-[18px] border border-[#F0EBE8] bg-white py-12 text-center text-[13px] text-[#A0A0A0]">
              불러오는 중...
            </div>
          ) : filtered.length === 0 ? (
            <div className="rounded-[18px] border border-[#F0EBE8] bg-white p-8 text-center">
              <div className="mb-3 text-[40px]">🧘‍♀️</div>
              <p className="mb-1 text-[15px] font-semibold text-[#2A2A2C]">
                {activeFilter === "ALL"
                  ? "출석 기록이 없어요"
                  : "해당 기록이 없어요"}
              </p>
              <p className="text-[13px] text-[#6B6B6B]">
                수업을 예약하고 첫 출석을 시작해보세요!
              </p>
              <Link
                href="/schedule"
                className="mt-4 inline-block rounded-[8px] px-4 py-2 text-[13px] font-semibold text-white"
                style={{
                  background:
                    "linear-gradient(135deg, #F0A0B5 0%, #D88A9E 100%)",
                }}
              >
                수업 예약하기
              </Link>
            </div>
          ) : (
            <div className="flex flex-col gap-5">
              {groupedByMonth.map(([key, group]) => (
                <div key={key}>
                  <div className="mb-2 text-[12px] font-semibold tracking-[-0.01em] text-[#6B6B6B]">
                    {group.label}
                  </div>
                  <div className="flex flex-col gap-2">
                    {group.rows.map((row) => {
                      const r = row.reservation;
                      const meta = KIND_META[row.kind];
                      const d = new Date(`${r.classDate}T00:00:00`);
                      const day = String(d.getDate()).padStart(2, "0");
                      const week = WEEKDAY_KO[d.getDay()];
                      return (
                        <div
                          key={r.id}
                          className="flex items-center gap-3.5 rounded-[14px] border border-[#F0EBE8] bg-white p-3"
                        >
                          <div className="flex h-12 w-12 flex-shrink-0 flex-col items-center justify-center rounded-[10px] bg-[#FAF7F5]">
                            <span className="text-[16px] font-extrabold leading-none tracking-[-0.03em] text-[#2A2A2C]">
                              {day}
                            </span>
                            <span className="mt-0.5 text-[10px] font-medium text-[#A0A0A0]">
                              {week}
                            </span>
                          </div>
                          <div className="min-w-0 flex-1">
                            <div className="text-[14px] font-semibold tracking-[-0.01em] text-[#2A2A2C]">
                              {r.lessonTypeName}
                            </div>
                            <div className="mt-0.5 flex items-center gap-1.5 text-[12px] text-[#6B6B6B]">
                              <span>
                                {r.startTime.slice(0, 5)} - {r.endTime.slice(0, 5)}
                              </span>
                              <span className="text-[#D9D2CD]">·</span>
                              <span className="truncate">{r.instructorName}</span>
                            </div>
                          </div>
                          <span
                            className="flex-shrink-0 rounded-full px-2.5 py-1 text-[11px] font-bold"
                            style={{
                              background: meta.bg,
                              color: meta.color,
                            }}
                          >
                            <span
                              className="mr-1 inline-block h-1.5 w-1.5 rounded-full align-middle"
                              style={{ background: meta.dot }}
                            />
                            {meta.label}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>

      <MobileTabBar />
    </div>
  );
}
