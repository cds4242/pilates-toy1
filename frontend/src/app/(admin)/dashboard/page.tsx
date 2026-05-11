"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { adminApi } from "@/lib/api/admin";
import { api } from "@/lib/api/client";
import type { DashboardData } from "@/lib/types/domain";
import { usePageTitle } from "@/lib/hooks/use-page-title";
import { useAuth } from "@/lib/hooks/use-auth";

interface RevenueData {
  total: number;
  breakdown: { date: string; amount: number }[];
}

const WEEKDAY_KO = ["일", "월", "화", "수", "목", "금", "토"];

function formatDateText(d: Date) {
  return `${d.getFullYear()} · ${d.getMonth() + 1}월 ${d.getDate()}일 ${WEEKDAY_KO[d.getDay()]}요일`;
}

function HHMM(time: string) {
  return time.slice(0, 5);
}

export default function AdminDashboardPage() {
  usePageTitle("대시보드");
  const router = useRouter();
  const { user } = useAuth();

  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [view, setView] = useState<"list" | "calendar">("list");
  const [searchQuery, setSearchQuery] = useState("");

  const [revPeriod, setRevPeriod] = useState<"week" | "month">("week");
  const [revOffset] = useState(0);
  const [revenue, setRevenue] = useState<RevenueData | null>(null);
  const [revLoading, setRevLoading] = useState(false);
  const revenueRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    async function load() {
      try {
        setData(await adminApi.getDashboard());
      } catch {
        // empty
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  useEffect(() => {
    async function loadRevenue() {
      setRevLoading(true);
      try {
        const res = await api<RevenueData>(
          "get",
          "/api/admin/dashboard/revenue",
          { period: revPeriod, offset: revOffset },
        );
        setRevenue(res);
      } catch {
        // empty
      } finally {
        setRevLoading(false);
      }
    }
    loadRevenue();
  }, [revPeriod, revOffset]);

  const today = new Date();
  const dateText = formatDateText(today);
  const todayCount = data?.todayClasses.count ?? 0;
  const todayReservations =
    data?.todayClasses.schedules.reduce((s, c) => s + c.reservedCount, 0) ?? 0;
  const weekRevenue = Number(data?.thisWeekRevenue.total ?? 0);
  const expiringCount = data?.expiringMemberships.length ?? 0;

  const ALERT_LIMIT = 4;
  const noShowAlerts = data?.alerts.noShowMembers ?? [];
  const expiringAlerts = data?.expiringMemberships.slice(0, 3) ?? [];
  const lowMembershipAlerts = data?.alerts.lowMembershipMembers.slice(0, 3) ?? [];
  const allAlerts = [
    ...noShowAlerts.map((m) => ({
      kind: "danger" as const,
      title: `${m.memberName} 노쇼`,
      desc: `최근 노쇼 ${m.noShowCount}회`,
      time: "오늘",
      iconType: "warning" as const,
    })),
    ...expiringAlerts.map((m) => ({
      kind: "warn" as const,
      title: `정기권 만료 임박 — ${m.memberName}`,
      desc: `D-${m.daysLeft} · ${m.passName}`,
      time: m.daysLeft <= 3 ? "임박" : "이번 주",
      iconType: "clock" as const,
    })),
    ...lowMembershipAlerts.map((m) => ({
      kind: "info" as const,
      title: `${m.memberName} 잔여 ${m.remainingCount}회`,
      desc: m.passName,
      time: "오늘",
      iconType: "user" as const,
    })),
  ].slice(0, ALERT_LIMIT);
  const totalAlertCount =
    noShowAlerts.length + expiringAlerts.length + lowMembershipAlerts.length;

  const filteredSchedules = useMemo(() => {
    if (!data) return [];
    const q = searchQuery.trim().toLowerCase();
    const list = [...data.todayClasses.schedules].sort((a, b) =>
      a.time.localeCompare(b.time),
    );
    if (!q) return list;
    return list.filter(
      (s) =>
        s.className.toLowerCase().includes(q) ||
        s.instructor.toLowerCase().includes(q),
    );
  }, [data, searchQuery]);

  const nowMinutes = today.getHours() * 60 + today.getMinutes();
  function isOngoing(time: string) {
    const [h, m] = time.split(":").map(Number);
    const start = h * 60 + (m || 0);
    return start <= nowMinutes && nowMinutes < start + 50;
  }

  const revBars = revenue?.breakdown ?? [];
  const revMax = Math.max(...revBars.map((b) => Number(b.amount)), 1);
  const revTotalManwon = Math.round(Number(revenue?.total ?? 0) / 10000);

  return (
    <div>
      {/* Page Header */}
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.1em] text-[#D88A9E]">
            {dateText}
          </div>
          <h1 className="text-[22px] font-bold leading-[1.2] tracking-[-0.03em] text-[#2A2A2C]">
            안녕하세요, {user?.name || "원장"}님 👋
          </h1>
          <p className="mt-1 text-[13px] text-[#6B6B6B]">
            {loading
              ? "오늘의 학원 현황을 불러오는 중이에요."
              : todayCount === 0
                ? "오늘 예정된 수업이 없어요. 시간표를 등록해보세요."
                : `오늘 ${todayCount}개 수업 · ${todayReservations}건 예약이 잡혀있어요.`}
          </p>
        </div>
        <div className="flex w-full items-center gap-2 md:w-auto">
          <div className="flex flex-1 items-center gap-2 rounded-full border border-[#F0EBE8] bg-white px-3.5 py-2 transition-colors focus-within:border-[#FAD4DE] md:w-56 md:flex-none">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={2}
              stroke="currentColor"
              className="h-4 w-4 flex-shrink-0 text-[#A0A0A0]"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z"
              />
            </svg>
            <input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="수업 · 강사 검색..."
              className="flex-1 bg-transparent text-[13px] text-[#2A2A2C] outline-none placeholder:text-[#A0A0A0]"
            />
          </div>
          <button
            onClick={() => router.push("/classes")}
            className="inline-flex items-center gap-1.5 rounded-full bg-[#F0A0B5] px-3.5 py-2 text-[13px] font-semibold tracking-[-0.01em] text-white transition-colors hover:bg-[#D88A9E]"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={2.4}
              stroke="currentColor"
              className="h-3.5 w-3.5"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M12 4.5v15m7.5-7.5h-15"
              />
            </svg>
            수업 추가
          </button>
        </div>
      </div>

      {/* KPI grid */}
      <div className="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {/* Feature card */}
        <Link
          href="/classes"
          className="group relative overflow-hidden rounded-[16px] p-4 text-white no-underline transition-transform hover:-translate-y-0.5"
          style={{
            background:
              "linear-gradient(135deg, #F8B5C5 0%, #EE94AB 60%, #D88A9E 100%)",
            boxShadow: "0 6px 24px rgba(216, 138, 158, 0.18)",
          }}
        >
          <span
            className="pointer-events-none absolute -right-[60px] -top-[60px] h-[160px] w-[160px] rounded-full"
            style={{
              background:
                "radial-gradient(circle, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0) 70%)",
            }}
          />
          <div className="relative z-[2] mb-2 flex items-center justify-between">
            <span className="text-[12px] font-medium tracking-[-0.01em] text-white/85">
              오늘 수업
            </span>
            <span className="flex h-[26px] w-[26px] items-center justify-center rounded-[8px] bg-white/20 text-white">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.8}
                stroke="currentColor"
                className="h-4 w-4"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5"
                />
              </svg>
            </span>
          </div>
          <div className="relative z-[2] text-[24px] font-extrabold leading-none tracking-[-0.03em] text-white">
            {loading ? "—" : todayCount}
            <small className="ml-0.5 text-[12px] font-medium text-white/85">
              건
            </small>
          </div>
          <span className="relative z-[2] mt-2 inline-flex items-center gap-0.5 rounded-full bg-white/20 px-2 py-0.5 text-[11px] font-semibold text-white">
            예약 {todayReservations}건
          </span>
        </Link>

        {/* Today reservations */}
        <Link
          href="/members"
          className="group relative overflow-hidden rounded-[16px] border border-[#F0EBE8] bg-white p-4 no-underline transition-all hover:-translate-y-0.5 hover:border-[#FAD4DE] hover:shadow-[0_1px_2px_rgba(45,30,30,0.04),0_4px_16px_rgba(45,30,30,0.04)]"
        >
          <div className="mb-2 flex items-center justify-between">
            <span className="text-[12px] font-medium tracking-[-0.01em] text-[#A0A0A0]">
              오늘 예약
            </span>
            <span className="flex h-[26px] w-[26px] items-center justify-center rounded-[8px] bg-[#FDEDF2] text-[#D88A9E]">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.8}
                stroke="currentColor"
                className="h-4 w-4"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                />
              </svg>
            </span>
          </div>
          <div className="text-[24px] font-extrabold leading-none tracking-[-0.03em] text-[#2A2A2C]">
            {loading ? "—" : todayReservations}
            <small className="ml-0.5 text-[12px] font-medium text-[#6B6B6B]">
              건
            </small>
          </div>
          <span className="mt-2 inline-flex items-center gap-0.5 rounded-full bg-[rgba(76,175,80,0.1)] px-2 py-0.5 text-[11px] font-semibold text-[#4CAF50]">
            전체 회원 보기
          </span>
        </Link>

        {/* Week revenue */}
        <div
          onClick={() =>
            revenueRef.current?.scrollIntoView({ behavior: "smooth" })
          }
          className="group relative cursor-pointer overflow-hidden rounded-[16px] border border-[#F0EBE8] bg-white p-4 transition-all hover:-translate-y-0.5 hover:border-[#FAD4DE] hover:shadow-[0_1px_2px_rgba(45,30,30,0.04),0_4px_16px_rgba(45,30,30,0.04)]"
        >
          <div className="mb-2 flex items-center justify-between">
            <span className="text-[12px] font-medium tracking-[-0.01em] text-[#A0A0A0]">
              이번 주 매출
            </span>
            <span className="flex h-[26px] w-[26px] items-center justify-center rounded-[8px] bg-[#FDEDF2] text-[#D88A9E]">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.8}
                stroke="currentColor"
                className="h-4 w-4"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M2.25 18.75a60.07 60.07 0 0 1 15.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 0 1 3 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 0 0-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 0 1-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 0 0 3 15h-.75M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"
                />
              </svg>
            </span>
          </div>
          <div className="text-[24px] font-extrabold leading-none tracking-[-0.03em] text-[#2A2A2C]">
            {loading ? "—" : Math.round(weekRevenue / 10000).toLocaleString()}
            <small className="ml-0.5 text-[12px] font-medium text-[#6B6B6B]">
              만원
            </small>
          </div>
          <span className="mt-2 inline-flex items-center gap-0.5 rounded-full bg-[rgba(76,175,80,0.1)] px-2 py-0.5 text-[11px] font-semibold text-[#4CAF50]">
            차트에서 자세히 보기
          </span>
        </div>

        {/* Expiring */}
        <Link
          href="/members?quick=expiring"
          className="group relative overflow-hidden rounded-[16px] border border-[#F0EBE8] bg-white p-4 no-underline transition-all hover:-translate-y-0.5 hover:border-[#FAD4DE] hover:shadow-[0_1px_2px_rgba(45,30,30,0.04),0_4px_16px_rgba(45,30,30,0.04)]"
        >
          <div className="mb-2 flex items-center justify-between">
            <span className="text-[12px] font-medium tracking-[-0.01em] text-[#A0A0A0]">
              만료 임박
            </span>
            <span className="flex h-[26px] w-[26px] items-center justify-center rounded-[8px] bg-[#FDEDF2] text-[#D88A9E]">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.8}
                stroke="currentColor"
                className="h-4 w-4"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                />
              </svg>
            </span>
          </div>
          <div className="text-[24px] font-extrabold leading-none tracking-[-0.03em] text-[#2A2A2C]">
            {loading ? "—" : expiringCount}
            <small className="ml-0.5 text-[12px] font-medium text-[#6B6B6B]">
              명
            </small>
          </div>
          <span
            className={`mt-2 inline-flex items-center gap-0.5 rounded-full px-2 py-0.5 text-[11px] font-semibold ${
              expiringCount > 0
                ? "bg-[rgba(231,111,81,0.1)] text-[#E76F51]"
                : "bg-[rgba(76,175,80,0.1)] text-[#4CAF50]"
            }`}
          >
            {expiringCount > 0 ? "7일 이내 만료" : "이상 없음"}
          </span>
        </Link>
      </div>

      {/* Content grid */}
      <div className="mb-4 grid grid-cols-1 gap-3 lg:grid-cols-[1.3fr_1fr]">
        {/* Today's classes */}
        <div className="rounded-[16px] border border-[#F0EBE8] bg-white p-4">
          <div className="mb-3 flex items-center justify-between gap-3">
            <div className="min-w-0">
              <span className="mb-0.5 block text-[10px] font-bold uppercase tracking-[0.1em] text-[#D88A9E]">
                TODAY
              </span>
              <h2 className="text-[15px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
                오늘의 수업
              </h2>
            </div>
            <div className="flex gap-0.5 rounded-full bg-[#FAF7F5] p-[3px]">
              <button
                onClick={() => setView("list")}
                className={`cursor-pointer rounded-full border-none px-3.5 py-1.5 text-[12px] font-semibold transition-all ${
                  view === "list"
                    ? "bg-white text-[#D88A9E] shadow-[0_1px_3px_rgba(45,30,30,0.06)]"
                    : "bg-transparent text-[#A0A0A0]"
                }`}
              >
                리스트
              </button>
              <button
                onClick={() => setView("calendar")}
                className={`cursor-pointer rounded-full border-none px-3.5 py-1.5 text-[12px] font-semibold transition-all ${
                  view === "calendar"
                    ? "bg-white text-[#D88A9E] shadow-[0_1px_3px_rgba(45,30,30,0.06)]"
                    : "bg-transparent text-[#A0A0A0]"
                }`}
              >
                캘린더
              </button>
            </div>
          </div>

          {view === "list" ? (
            filteredSchedules.length === 0 ? (
              <div className="py-6 text-center text-[12px] text-[#A0A0A0]">
                {loading ? "불러오는 중..." : "예정된 수업이 없습니다"}
              </div>
            ) : (
              <div className="flex flex-col gap-2">
                {filteredSchedules.map((s, i) => {
                  const ongoing = isOngoing(s.time);
                  const ratio =
                    s.capacity > 0 ? (s.reservedCount / s.capacity) * 100 : 0;
                  const isFull = s.reservedCount >= s.capacity;
                  return (
                    <div
                      key={i}
                      onClick={() => router.push("/classes")}
                      className={`flex cursor-pointer items-center gap-2.5 rounded-[12px] border px-3 py-2 transition-all ${
                        ongoing
                          ? "border-[#FAD4DE]"
                          : "border-[#F0EBE8] bg-white hover:border-[#FAD4DE] hover:bg-[#FFF7F4]"
                      }`}
                      style={
                        ongoing
                          ? {
                              background:
                                "linear-gradient(180deg, #FFF8F5 0%, #FFFFFF 100%)",
                            }
                          : undefined
                      }
                    >
                      <span
                        className={`flex h-8 min-w-[50px] items-center justify-center rounded-md px-1 text-[13px] font-extrabold tracking-[-0.02em] ${
                          ongoing
                            ? "bg-[#F0A0B5] text-white"
                            : "bg-[#FDEDF2] text-[#D88A9E]"
                        }`}
                      >
                        {HHMM(s.time)}
                      </span>
                      <span className="truncate text-[14px] font-semibold tracking-[-0.01em] text-[#2A2A2C]">
                        {s.className}
                      </span>
                      <span className="flex-shrink-0 text-[13px] text-[#6B6B6B]">
                        · {s.instructor}
                      </span>
                      {ongoing && (
                        <span className="flex-shrink-0 rounded-full border border-[#FAD4DE] bg-white px-1.5 py-px text-[10px] font-bold tracking-[0.04em] text-[#D88A9E]">
                          진행 중
                        </span>
                      )}
                      <div className="ml-auto flex flex-shrink-0 items-center gap-2">
                        <div className="h-[5px] w-14 overflow-hidden rounded-full bg-[#F0EBE8]">
                          <div
                            className="h-full rounded-full transition-all"
                            style={{
                              width: `${ratio}%`,
                              background: isFull ? "#E76F51" : "#F0A0B5",
                            }}
                          />
                        </div>
                        <span className="min-w-[36px] text-right text-[12px] font-semibold tracking-[-0.01em] text-[#A0A0A0]">
                          <strong className="font-bold text-[#2A2A2C]">
                            {s.reservedCount}
                          </strong>
                          /{s.capacity}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )
          ) : (
            <CalendarGrid schedules={filteredSchedules} />
          )}
        </div>

        {/* Notifications */}
        <div className="rounded-[18px] border border-[#F0EBE8] bg-white p-[22px]">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div className="min-w-0">
              <span className="mb-0.5 block text-[10px] font-bold uppercase tracking-[0.1em] text-[#D88A9E]">
                NOTIFICATIONS
              </span>
              <h2 className="text-[15px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
                알림
              </h2>
            </div>
            {totalAlertCount > ALERT_LIMIT && (
              <span className="rounded-full border border-[#F0EBE8] bg-[#FAF7F5] px-2.5 py-0.5 text-[11px] font-semibold text-[#6B6B6B]">
                +{totalAlertCount - ALERT_LIMIT}건 더
              </span>
            )}
          </div>
          {allAlerts.length === 0 ? (
            <p className="py-4 text-center text-[12px] text-[#A0A0A0]">
              {loading ? "불러오는 중..." : "새로운 알림이 없어요"}
            </p>
          ) : (
            <div className="flex flex-col gap-2">
              {allAlerts.map((a, i) => (
                <div
                  key={i}
                  onClick={() => router.push("/members")}
                  className="flex cursor-pointer items-center gap-2.5 rounded-[12px] border border-transparent bg-[#FAF7F5] px-3 py-2 transition-all hover:border-[#F0EBE8] hover:bg-white"
                >
                  <div
                    className={`flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-[8px] ${
                      a.kind === "warn"
                        ? "bg-[rgba(244,162,97,0.14)] text-[#F4A261]"
                        : a.kind === "danger"
                          ? "bg-[rgba(231,111,81,0.12)] text-[#E76F51]"
                          : "bg-[#FDEDF2] text-[#D88A9E]"
                    }`}
                  >
                    <AlertIcon type={a.iconType} />
                  </div>
                  <span className="truncate text-[13px] font-semibold tracking-[-0.01em] text-[#2A2A2C]">
                    {a.title}
                  </span>
                  <span className="truncate text-[12px] text-[#6B6B6B]">
                    · {a.desc}
                  </span>
                  <span className="ml-auto flex-shrink-0 text-[11px] text-[#A0A0A0]">
                    {a.time}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Revenue chart */}
      <div
        ref={revenueRef}
        className="rounded-[16px] border border-[#F0EBE8] bg-white p-4"
      >
        <div className="mb-2 flex items-center justify-between gap-3">
          <div className="min-w-0">
            <span className="mb-0.5 block text-[10px] font-bold uppercase tracking-[0.1em] text-[#D88A9E]">
              REVENUE
            </span>
            <h2 className="text-[15px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
              매출 추이
            </h2>
          </div>
          <div className="flex gap-0.5 rounded-full bg-[#FAF7F5] p-[3px]">
            <button
              onClick={() => setRevPeriod("week")}
              className={`cursor-pointer rounded-full border-none px-3.5 py-1.5 text-[12px] font-semibold transition-all ${
                revPeriod === "week"
                  ? "bg-white text-[#D88A9E] shadow-[0_1px_3px_rgba(45,30,30,0.06)]"
                  : "bg-transparent text-[#A0A0A0]"
              }`}
            >
              주간
            </button>
            <button
              onClick={() => setRevPeriod("month")}
              className={`cursor-pointer rounded-full border-none px-3.5 py-1.5 text-[12px] font-semibold transition-all ${
                revPeriod === "month"
                  ? "bg-white text-[#D88A9E] shadow-[0_1px_3px_rgba(45,30,30,0.06)]"
                  : "bg-transparent text-[#A0A0A0]"
              }`}
            >
              월간
            </button>
          </div>
        </div>
        <div className="mb-3 flex flex-wrap items-baseline gap-2">
          <div className="text-[22px] font-extrabold tracking-[-0.03em] text-[#2A2A2C]">
            {revLoading ? "—" : revTotalManwon.toLocaleString()}
            <small className="ml-0.5 text-[12px] font-medium text-[#6B6B6B]">
              만원
            </small>
          </div>
          <span className="inline-flex items-center gap-0.5 rounded-full bg-[rgba(76,175,80,0.1)] px-2 py-0.5 text-[11px] font-semibold text-[#4CAF50]">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={2.4}
              stroke="currentColor"
              className="h-3 w-3"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="m4.5 15.75 7.5-7.5 7.5 7.5"
              />
            </svg>
            {revPeriod === "week" ? "이번 주" : "이번 달"} 합계
          </span>
        </div>
        {revLoading ? (
          <div className="flex h-[125px] items-center justify-center text-[12px] text-[#A0A0A0]">
            불러오는 중...
          </div>
        ) : revBars.length === 0 ? (
          <div className="flex h-[125px] items-center justify-center text-[12px] text-[#A0A0A0]">
            매출 데이터가 없습니다
          </div>
        ) : (
          <div className="relative mt-1 flex h-[125px] items-end gap-2 pt-3">
            {revBars.map((b, i) => {
              const amt = Number(b.amount);
              const heightPct = Math.max(4, (amt / revMax) * 100);
              const isPeak = amt === revMax && revMax > 0;
              const d = new Date(b.date);
              const barMaxWidth = revPeriod === "week" ? 56 : 26;
              return (
                <div
                  key={i}
                  className="group relative flex h-full flex-1 flex-col items-center justify-end gap-1.5"
                >
                  <div className="absolute -top-1 z-10 hidden whitespace-nowrap rounded-md bg-[#2A2A2C] px-2 py-1 text-[11px] text-white group-hover:block">
                    {amt.toLocaleString()}원
                  </div>
                  <div
                    className="w-full cursor-pointer rounded-t-md transition-transform hover:origin-bottom hover:scale-y-[1.02]"
                    style={{
                      height: `${heightPct}%`,
                      maxWidth: `${barMaxWidth}px`,
                      background: isPeak
                        ? "linear-gradient(180deg, #D88A9E 0%, #F0A0B5 100%)"
                        : "linear-gradient(180deg, #F0A0B5 0%, #FAD4DE 100%)",
                      borderRadius: "6px 6px 2px 2px",
                    }}
                  />
                  <span className="text-[10px] font-medium text-[#A0A0A0]">
                    {revPeriod === "week"
                      ? WEEKDAY_KO[d.getDay()]
                      : `${d.getDate()}일`}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

function AlertIcon({ type }: { type: "warning" | "clock" | "user" }) {
  if (type === "warning") {
    return (
      <svg
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        strokeWidth={1.8}
        stroke="currentColor"
        className="h-4 w-4"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
        />
      </svg>
    );
  }
  if (type === "clock") {
    return (
      <svg
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        strokeWidth={1.8}
        stroke="currentColor"
        className="h-4 w-4"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
        />
      </svg>
    );
  }
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.8}
      stroke="currentColor"
      className="h-4 w-4"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M18 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0ZM3 19.235v-.11a6.375 6.375 0 0 1 12.75 0v.109A12.318 12.318 0 0 1 9.374 21c-2.331 0-4.512-.645-6.374-1.766Z"
      />
    </svg>
  );
}

function CalendarGrid({
  schedules,
}: {
  schedules: { time: string; instructor: string; className: string }[];
}) {
  const slotMap = new Map<string, { className: string; instructor: string }>();
  for (const s of schedules) {
    slotMap.set(HHMM(s.time), { className: s.className, instructor: s.instructor });
  }
  const slots = Array.from(slotMap.keys()).sort();
  if (slots.length === 0) {
    return (
      <div className="py-8 text-center text-[13px] text-[#A0A0A0]">
        오늘 예정된 수업이 없습니다
      </div>
    );
  }
  const today = new Date();
  const dayIdx = (today.getDay() + 6) % 7; // Mon=0
  const headerDays = ["월", "화", "수", "목", "금", "토", "일"];

  return (
    <div className="grid overflow-hidden rounded-[14px] border border-[#F0EBE8]" style={{ gridTemplateColumns: "56px repeat(7, 1fr)" }}>
      <div className="border-b border-[#F0EBE8] bg-[#FAF7F5] px-1 py-2.5 text-center text-[12px] font-semibold text-[#A0A0A0]" />
      {headerDays.map((d) => (
        <div
          key={d}
          className="border-b border-[#F0EBE8] bg-[#FAF7F5] px-1 py-2.5 text-center text-[12px] font-semibold text-[#A0A0A0]"
        >
          {d}
        </div>
      ))}
      {slots.map((time) => (
        <CalendarRow
          key={time}
          time={time}
          dayIdx={dayIdx}
          item={slotMap.get(time)!}
        />
      ))}
    </div>
  );
}

function CalendarRow({
  time,
  dayIdx,
  item,
}: {
  time: string;
  dayIdx: number;
  item: { className: string; instructor: string };
}) {
  return (
    <>
      <div className="flex items-center justify-center border-b border-r border-[#F0EBE8] bg-[#FAF7F5] px-1 py-2 text-[11px] font-medium text-[#A0A0A0]">
        {time}
      </div>
      {Array.from({ length: 7 }, (_, i) => {
        const last = i === 6;
        return (
          <div
            key={i}
            className={`min-h-[44px] border-b border-[#F0EBE8] p-1 ${
              last ? "" : "border-r"
            }`}
          >
            {i === dayIdx && (
              <div className="cursor-pointer overflow-hidden text-ellipsis whitespace-nowrap rounded-md border border-transparent bg-[#FDEDF2] px-1.5 py-1 text-[11px] font-semibold text-[#D88A9E] transition-all hover:bg-[#F0A0B5] hover:text-white">
                {item.className.slice(0, 2)}·{item.instructor.slice(0, 3)}
              </div>
            )}
          </div>
        );
      })}
    </>
  );
}
