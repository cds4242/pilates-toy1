"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/store/auth-store";
import { memberApi } from "@/lib/api/member";
import type { Member, Membership, Reservation } from "@/lib/types/domain";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { SearchSheet } from "@/components/design/SearchSheet";
import { usePageTitle } from "@/lib/hooks/use-page-title";

const WEEKDAY_KO = ["일", "월", "화", "수", "목", "금", "토"];
const WEEKDAY_EN = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];

function formatDateChip(dateStr: string) {
  const d = new Date(dateStr);
  return {
    month: `${d.getMonth() + 1}월`,
    day: String(d.getDate()).padStart(2, "0"),
    week: WEEKDAY_EN[d.getDay()],
  };
}

function formatTimeRange(start: string, end: string) {
  return `${start.slice(0, 5)} - ${end.slice(0, 5)}`;
}

function diffDays(endDate: string) {
  return Math.ceil((new Date(endDate).getTime() - Date.now()) / 86400000);
}

function relativeBadge(dateStr: string) {
  const d = new Date(dateStr);
  d.setHours(0, 0, 0, 0);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const diff = Math.round((d.getTime() - today.getTime()) / 86400000);
  if (diff === 0) return "오늘";
  if (diff === 1) return "내일";
  return null;
}

export default function MemberHomePage() {
  usePageTitle("홈");
  const router = useRouter();
  const { user, accessToken, _hydrated } = useAuthStore();

  useEffect(() => {
    if (!_hydrated) return;
    if (!accessToken) router.replace("/login");
  }, [accessToken, router, _hydrated]);

  const [member, setMember] = useState<Member | null>(null);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchOpen, setSearchOpen] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const [me, ms, rs] = await Promise.all([
          memberApi.getMe(),
          memberApi.getMemberships(),
          memberApi.getReservations(),
        ]);
        setMember(me);
        setMemberships(ms);
        setReservations(rs);
      } catch {
        // empty
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const activeMembership = memberships.find((m) => m.status === "ACTIVE");
  const todayStr = new Date().toISOString().split("T")[0];
  const upcomingReservations = reservations
    .filter((r) => r.status === "CONFIRMED" && r.classDate >= todayStr)
    .sort(
      (a, b) =>
        a.classDate.localeCompare(b.classDate) ||
        a.startTime.localeCompare(b.startTime),
    )
    .slice(0, 3);

  const today = new Date();
  const dateText = `${today.getFullYear()}년 ${today.getMonth() + 1}월 ${today.getDate()}일 ${WEEKDAY_KO[today.getDay()]}요일`;

  const memberName = member?.name || user?.name || "회원";
  const daysLeft = activeMembership ? diffDays(activeMembership.endDate) : 0;
  const expiryText = activeMembership
    ? `${activeMembership.endDate.replace(/-/g, ".")} 만료${daysLeft >= 0 ? ` · D-${daysLeft}` : ""}`
    : "";

  const remaining = activeMembership?.unlimited
    ? null
    : activeMembership?.remainingCount ?? 0;
  const totalCount = activeMembership?.totalCount ?? 0;
  const usedCount = activeMembership && !activeMembership.unlimited
    ? Math.max(0, totalCount - (activeMembership.remainingCount ?? 0))
    : 0;
  const percent = activeMembership?.unlimited
    ? 100
    : totalCount > 0
      ? Math.round(((remaining ?? 0) / totalCount) * 100)
      : 0;

  // Stats
  const monthAttended = reservations.filter((r) => {
    const d = new Date(r.classDate);
    const now = new Date();
    return (
      d.getMonth() === now.getMonth() &&
      d.getFullYear() === now.getFullYear() &&
      r.status === "CONFIRMED" &&
      r.classDate <= todayStr
    );
  }).length;
  const monthPlanned = reservations.filter((r) => {
    const d = new Date(r.classDate);
    const now = new Date();
    return (
      d.getMonth() === now.getMonth() &&
      d.getFullYear() === now.getFullYear() &&
      r.status === "CONFIRMED"
    );
  }).length;
  const attendanceRate = monthPlanned > 0 ? Math.round((monthAttended / monthPlanned) * 100) : 0;

  // ring
  const RING_RADIUS = 34;
  const RING_CIRC = 2 * Math.PI * RING_RADIUS; // ≈ 213.6
  const ringOffset = RING_CIRC * (1 - (percent / 100));

  return (
    <div className="relative mx-auto max-w-[480px] min-h-screen bg-white pb-24 overflow-hidden">
      {/* ---------- Hero ---------- */}
      <section
        className="relative overflow-hidden px-6 pt-[18px] pb-7"
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

        {/* Header */}
        <div className="relative z-[2] flex items-center justify-between">
          <div className="flex items-center gap-2 text-[16px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
            <div
              className="flex h-7 w-7 items-center justify-center rounded-[8px] text-[14px] font-extrabold italic text-white"
              style={{ background: "#F0A0B5", fontFamily: "Georgia, serif" }}
            >
              P
            </div>
            필라테스 OO점
          </div>
          <div className="flex items-center gap-1.5">
            <button
              aria-label="수업 검색"
              onClick={() => setSearchOpen(true)}
              className="relative flex h-10 w-10 items-center justify-center rounded-full border border-[rgba(240,235,232,0.6)] bg-white/70 backdrop-blur-md transition-colors hover:bg-white"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.8}
                stroke="currentColor"
                className="h-5 w-5 text-[#2A2A2C]"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z"
                />
              </svg>
            </button>
            <button
              aria-label="알림"
              onClick={() => router.push("/notifications")}
              className="relative flex h-10 w-10 items-center justify-center rounded-full border border-[rgba(240,235,232,0.6)] bg-white/70 backdrop-blur-md transition-colors hover:bg-white"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.8}
                stroke="currentColor"
                className="h-5 w-5 text-[#2A2A2C]"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0"
                />
              </svg>
              <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full border-2 border-white bg-[#E76F51]" />
            </button>
          </div>
        </div>

        {/* Greeting */}
        <div className="relative z-[2] mt-[26px]">
          <div className="mb-1.5 text-[13px] font-medium tracking-[0.01em] text-[#A0A0A0]">
            {dateText}
          </div>
          <h1 className="text-[26px] font-bold leading-[1.3] tracking-[-0.03em] text-[#2A2A2C]">
            <em className="not-italic text-[#D88A9E]">{memberName}</em>님,
            <br />
            오늘도 가볍게 시작해볼까요?
          </h1>
          <p className="mt-1.5 text-[14px] text-[#6B6B6B]">
            {loading
              ? "수업 정보를 불러오는 중이에요."
              : upcomingReservations.length > 0
                ? `이번 주 ${monthAttended}회 출석, 다음 수업까지 ${(() => {
                    const d = new Date(upcomingReservations[0].classDate);
                    d.setHours(0, 0, 0, 0);
                    const t = new Date();
                    t.setHours(0, 0, 0, 0);
                    const diff = Math.round((d.getTime() - t.getTime()) / 86400000);
                    return diff === 0 ? "오늘" : `${diff}일 남았어요`;
                  })()}.`
                : "오늘 예약된 수업이 없어요."}
          </p>
        </div>
      </section>

      <main className="relative z-[3] -mt-5 flex flex-col gap-7 px-5">
        {/* ---------- Pass Card ---------- */}
        {activeMembership ? (
          <Link
            href="/membership"
            className="relative block overflow-hidden rounded-[24px] p-[22px] text-white no-underline"
            style={{
              background:
                "linear-gradient(135deg, #F8B5C5 0%, #EE94AB 60%, #D88A9E 100%)",
              boxShadow: "0 6px 24px rgba(216, 138, 158, 0.18)",
            }}
          >
            <span
              className="pointer-events-none absolute -right-[90px] -top-[90px] h-[220px] w-[220px] rounded-full"
              style={{
                background:
                  "radial-gradient(circle, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0) 70%)",
              }}
            />
            <span
              className="pointer-events-none absolute -bottom-[60px] -left-[50px] h-[140px] w-[140px] rounded-full"
              style={{
                background:
                  "radial-gradient(circle, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0) 70%)",
              }}
            />

            <div className="relative z-[2] flex items-center justify-between">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-white/20 px-[11px] py-[5px] text-[11px] font-semibold tracking-[0.02em] backdrop-blur-md">
                <span className="h-1.5 w-1.5 rounded-full bg-white" />
                ACTIVE
              </span>
              <span className="text-[12px] font-medium text-white/85">
                {expiryText}
              </span>
            </div>

            <div className="relative z-[2] mt-[18px] flex items-center justify-between">
              <div className="flex-1">
                <div className="mb-1 text-[14px] font-medium text-white/90">
                  {activeMembership.passName}
                </div>
                <div className="flex items-baseline gap-1">
                  <span className="text-[44px] font-extrabold leading-none tracking-[-0.04em]">
                    {activeMembership.unlimited ? "∞" : remaining}
                  </span>
                  <span className="text-[14px] font-medium text-white/85">
                    {activeMembership.unlimited ? "무제한" : "회 남음"}
                  </span>
                </div>
                {!activeMembership.unlimited && (
                  <div className="mt-1.5 text-[12px] text-white/75">
                    사용 완료 {usedCount} / {totalCount}회
                  </div>
                )}
              </div>

              {/* Circular ring */}
              <div className="relative h-[76px] w-[76px] flex-shrink-0">
                <svg width="76" height="76" viewBox="0 0 76 76" className="-rotate-90">
                  <circle
                    cx="38"
                    cy="38"
                    r={RING_RADIUS}
                    fill="none"
                    stroke="rgba(255,255,255,0.25)"
                    strokeWidth={7}
                  />
                  <circle
                    cx="38"
                    cy="38"
                    r={RING_RADIUS}
                    fill="none"
                    stroke="#fff"
                    strokeWidth={7}
                    strokeLinecap="round"
                    strokeDasharray={RING_CIRC}
                    strokeDashoffset={ringOffset}
                    style={{ transition: "stroke-dashoffset 0.6s ease" }}
                  />
                </svg>
                <div className="absolute inset-0 flex flex-col items-center justify-center text-[14px] font-bold tracking-[-0.02em]">
                  {percent}%
                  <small className="mt-px text-[9px] font-medium tracking-[0.02em] opacity-85">
                    남음
                  </small>
                </div>
              </div>
            </div>

            <div className="relative z-[2] mt-[18px] flex items-center justify-between border-t border-dashed border-white/30 pt-4">
              <span className="flex items-center gap-1.5 text-[13px] font-medium">
                {daysLeft <= 14
                  ? "곧 만료돼요. 연장 혜택 보기"
                  : "수강권 정보 자세히 보기"}
              </span>
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-white/20">
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
                    d="m8.25 4.5 7.5 7.5-7.5 7.5"
                  />
                </svg>
              </span>
            </div>
          </Link>
        ) : !loading ? (
          <Link
            href="/membership"
            className="relative block overflow-hidden rounded-[24px] p-[22px] text-white no-underline"
            style={{
              background:
                "linear-gradient(135deg, #F8B5C5 0%, #EE94AB 60%, #D88A9E 100%)",
              boxShadow: "0 6px 24px rgba(216, 138, 158, 0.18)",
            }}
          >
            <div className="relative z-[2]">
              <div className="text-[14px] font-medium text-white/90">수강권이 없어요</div>
              <div className="mt-1 text-[22px] font-bold tracking-[-0.02em]">
                첫 수업 시작해볼까요?
              </div>
              <div className="mt-2 text-[13px] text-white/85">
                수강권을 등록하고 원하는 시간에 수업을 예약하세요
              </div>
            </div>
          </Link>
        ) : (
          <div
            className="h-[180px] animate-pulse rounded-[24px]"
            style={{ background: "#FAD4DE" }}
          />
        )}

        {/* ---------- Quick Actions ---------- */}
        <section>
          <div className="grid grid-cols-4 gap-2">
            <Link
              href="/schedule"
              className="flex cursor-pointer flex-col items-center gap-2 rounded-[18px] border border-[#F0EBE8] bg-white px-1.5 py-3.5 no-underline transition-[transform,border-color] duration-150 hover:-translate-y-0.5 hover:border-[#FAD4DE]"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#FDEDF2] text-[#D88A9E]">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.8}
                  stroke="currentColor"
                  className="h-[18px] w-[18px]"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5"
                  />
                </svg>
              </div>
              <span className="text-[12px] font-medium tracking-[-0.01em] text-[#2A2A2C]">
                예약하기
              </span>
            </Link>

            <Link
              href="/timetable"
              className="flex cursor-pointer flex-col items-center gap-2 rounded-[18px] border border-[#F0EBE8] bg-white px-1.5 py-3.5 no-underline transition-[transform,border-color] duration-150 hover:-translate-y-0.5 hover:border-[#FAD4DE]"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#FDEDF2] text-[#D88A9E]">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.8}
                  stroke="currentColor"
                  className="h-[18px] w-[18px]"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M3.75 3v18m16.5-18v18M3.75 7.5h16.5M3.75 12h16.5M3.75 16.5h16.5"
                  />
                </svg>
              </div>
              <span className="text-[12px] font-medium tracking-[-0.01em] text-[#2A2A2C]">
                시간표
              </span>
            </Link>

            <Link
              href="/membership"
              className="flex cursor-pointer flex-col items-center gap-2 rounded-[18px] border border-[#F0EBE8] bg-white px-1.5 py-3.5 no-underline transition-[transform,border-color] duration-150 hover:-translate-y-0.5 hover:border-[#FAD4DE]"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#FDEDF2] text-[#D88A9E]">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.8}
                  stroke="currentColor"
                  className="h-[18px] w-[18px]"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M16.5 6v.75m0 3v.75m0 3v.75m0 3V18m-9-5.25h5.25M7.5 15h3M3.375 5.25c-.621 0-1.125.504-1.125 1.125v3.026a3 3 0 0 1 0 5.198v3.026c0 .621.504 1.125 1.125 1.125h17.25c.621 0 1.125-.504 1.125-1.125v-3.026a3 3 0 0 1 0-5.198V6.375c0-.621-.504-1.125-1.125-1.125H3.375Z"
                  />
                </svg>
              </div>
              <span className="text-[12px] font-medium tracking-[-0.01em] text-[#2A2A2C]">
                정기권
              </span>
            </Link>

            <Link
              href="/attendance"
              className="flex cursor-pointer flex-col items-center gap-2 rounded-[18px] border border-[#F0EBE8] bg-white px-1.5 py-3.5 no-underline transition-[transform,border-color] duration-150 hover:-translate-y-0.5 hover:border-[#FAD4DE]"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#FDEDF2] text-[#D88A9E]">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.8}
                  stroke="currentColor"
                  className="h-[18px] w-[18px]"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                  />
                </svg>
              </div>
              <span className="text-[12px] font-medium tracking-[-0.01em] text-[#2A2A2C]">
                출석현황
              </span>
            </Link>
          </div>
        </section>

        {/* ---------- 다음 예약 ---------- */}
        <section>
          <div className="mb-3 flex items-baseline justify-between">
            <div>
              <span className="mb-1 block text-[11px] font-semibold uppercase tracking-[0.06em] text-[#D88A9E]">
                UPCOMING
              </span>
              <h2 className="text-[18px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
                다음 예약
              </h2>
            </div>
            <Link
              href="/reservations"
              className="flex items-center gap-0.5 text-[12px] font-medium text-[#A0A0A0] no-underline"
            >
              전체 보기
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={2}
                stroke="currentColor"
                className="h-3 w-3"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="m8.25 4.5 7.5 7.5-7.5 7.5"
                />
              </svg>
            </Link>
          </div>

          {upcomingReservations.length > 0 ? (
            <div className="flex flex-col gap-2.5">
              {upcomingReservations.map((r, idx) => {
                const chip = formatDateChip(r.classDate);
                const badge = relativeBadge(r.classDate);
                const isUpcoming = idx === 0;
                return (
                  <Link
                    key={r.id}
                    href="/reservations"
                    className={`flex items-center gap-3.5 rounded-[18px] border p-3.5 no-underline transition-[border-color,transform] duration-150 hover:-translate-y-px ${
                      isUpcoming
                        ? "border-[#FAD4DE]"
                        : "border-[#F0EBE8] bg-white hover:border-[#FAD4DE]"
                    }`}
                    style={
                      isUpcoming
                        ? {
                            background:
                              "linear-gradient(180deg, #FFF8F5 0%, #FFFFFF 100%)",
                          }
                        : undefined
                    }
                  >
                    <div
                      className={`flex h-16 w-14 flex-shrink-0 flex-col items-center justify-center rounded-[14px] border ${
                        isUpcoming
                          ? "border-[#F0A0B5] bg-[#F0A0B5]"
                          : "border-[rgba(240,160,181,0.15)] bg-[#FDEDF2]"
                      }`}
                    >
                      <span
                        className={`text-[10px] font-semibold tracking-[0.04em] ${
                          isUpcoming ? "text-white/95" : "text-[#D88A9E]"
                        }`}
                      >
                        {chip.month}
                      </span>
                      <span
                        className={`mt-0.5 text-[22px] font-extrabold leading-none tracking-[-0.03em] ${
                          isUpcoming ? "text-white" : "text-[#2A2A2C]"
                        }`}
                      >
                        {chip.day}
                      </span>
                      <span
                        className={`mt-0.5 text-[10px] ${
                          isUpcoming ? "text-white/85" : "text-[#A0A0A0]"
                        }`}
                      >
                        {chip.week}
                      </span>
                    </div>

                    <div className="min-w-0 flex-1">
                      <div
                        className={`mb-1 inline-flex items-center gap-1 text-[12px] font-medium ${
                          isUpcoming
                            ? "font-semibold text-[#D88A9E]"
                            : "text-[#A0A0A0]"
                        }`}
                      >
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          fill="none"
                          viewBox="0 0 24 24"
                          strokeWidth={2}
                          stroke="currentColor"
                          className="h-3 w-3"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                          />
                        </svg>
                        {formatTimeRange(r.startTime, r.endTime)}
                      </div>
                      <div className="mb-[3px] text-[15px] font-semibold tracking-[-0.01em] text-[#2A2A2C]">
                        {r.lessonTypeName}
                      </div>
                      <div className="flex items-center gap-2 text-[12px] text-[#6B6B6B]">
                        {r.instructorName} 강사
                      </div>
                    </div>

                    {badge && (
                      <span className="flex-shrink-0 rounded-full border border-[#FAD4DE] bg-white px-2 py-1 text-[10px] font-bold tracking-[0.04em] text-[#D88A9E]">
                        {badge}
                      </span>
                    )}
                  </Link>
                );
              })}
            </div>
          ) : (
            <div className="rounded-[18px] border border-[#F0EBE8] bg-white p-5 text-center">
              <p className="mb-1 text-[14px] font-semibold text-[#2A2A2C]">
                예정된 예약이 없어요
              </p>
              <p className="mb-3 text-[12px] text-[#6B6B6B]">
                원하는 시간에 수업을 예약해보세요
              </p>
              <Link
                href="/schedule"
                className="inline-block rounded-[8px] px-4 py-2 text-[13px] font-semibold text-white"
                style={{
                  background:
                    "linear-gradient(135deg, #F0A0B5 0%, #D88A9E 100%)",
                }}
              >
                수업 예약하기
              </Link>
            </div>
          )}
        </section>

        {/* ---------- 나의 기록 ---------- */}
        <section>
          <div className="mb-3 flex items-baseline justify-between">
            <div>
              <span className="mb-1 block text-[11px] font-semibold uppercase tracking-[0.06em] text-[#D88A9E]">
                THIS MONTH
              </span>
              <h2 className="text-[18px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
                나의 기록
              </h2>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2.5">
            <div className="rounded-[18px] border border-[#F0EBE8] bg-white p-4">
              <div className="mb-2.5 flex h-7 w-7 items-center justify-center rounded-lg bg-[#FDEDF2] text-[#D88A9E]">
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
                    d="M15.59 14.37a6 6 0 0 1-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 0 0 6.16-12.12A14.98 14.98 0 0 0 9.631 8.41m5.96 5.96a14.926 14.926 0 0 1-5.841 2.58m-.119-8.54a6 6 0 0 0-7.381 5.84h4.8m2.581-5.84a14.927 14.927 0 0 0-2.58 5.84m2.699 2.7c-.103.021-.207.041-.311.06a15.09 15.09 0 0 1-2.448-2.448 14.9 14.9 0 0 1 .06-.312m-2.24 2.39a4.493 4.493 0 0 0-1.757 4.306 4.493 4.493 0 0 0 4.306-1.758M16.5 9a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0Z"
                  />
                </svg>
              </div>
              <div className="mb-1 text-[12px] text-[#A0A0A0]">출석</div>
              <div className="text-[22px] font-extrabold tracking-[-0.03em] text-[#2A2A2C]">
                {monthAttended}
                <small className="ml-0.5 text-[12px] font-medium text-[#6B6B6B]">
                  / {monthPlanned || monthAttended}회
                </small>
              </div>
              <div className="mt-0.5 text-[11px] font-semibold text-[#4CAF50]">
                ↑ 출석률 {attendanceRate}%
              </div>
            </div>

            <div className="rounded-[18px] border border-[#F0EBE8] bg-white p-4">
              <div className="mb-2.5 flex h-7 w-7 items-center justify-center rounded-lg bg-[#FDEDF2] text-[#D88A9E]">
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
                    d="M15.362 5.214A8.252 8.252 0 0 1 12 21 8.25 8.25 0 0 1 6.038 7.047 8.287 8.287 0 0 0 9 9.601a8.983 8.983 0 0 1 3.361-6.867 8.21 8.21 0 0 0 3.001 2.48Z"
                  />
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M12 18a3.75 3.75 0 0 0 .495-7.468 5.99 5.99 0 0 0-1.925 3.547 5.975 5.975 0 0 1-2.133-1.001A3.75 3.75 0 0 0 12 18Z"
                  />
                </svg>
              </div>
              <div className="mb-1 text-[12px] text-[#A0A0A0]">잔여 횟수</div>
              <div className="text-[22px] font-extrabold tracking-[-0.03em] text-[#2A2A2C]">
                {activeMembership?.unlimited
                  ? "∞"
                  : (activeMembership?.remainingCount ?? 0)}
                <small className="ml-0.5 text-[12px] font-medium text-[#6B6B6B]">
                  {activeMembership?.unlimited ? "" : "회"}
                </small>
              </div>
              <div className="mt-0.5 text-[11px] font-semibold text-[#4CAF50]">
                {activeMembership
                  ? daysLeft > 0
                    ? `D-${daysLeft} 만료`
                    : "만료됨"
                  : "수강권 없음"}
              </div>
            </div>
          </div>
        </section>

        {/* ---------- Promo ---------- */}
        <section>
          <div className="mb-3">
            <span className="mb-1 block text-[11px] font-semibold uppercase tracking-[0.06em] text-[#D88A9E]">
              RECOMMENDED
            </span>
            <h2 className="text-[18px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
              이런 수업 어때요?
            </h2>
          </div>
          <Link
            href="/schedule"
            className="relative block h-[168px] overflow-hidden rounded-[24px] no-underline"
            style={{ background: "#2A2A2C" }}
          >
            <div
              className="absolute inset-0"
              style={{
                background:
                  "linear-gradient(135deg, #C97D8F 0%, #6B4953 60%, #2A2A2C 100%)",
                opacity: 0.95,
              }}
            />
            <div
              className="absolute inset-0"
              style={{
                background:
                  "linear-gradient(180deg, rgba(0,0,0,0) 30%, rgba(0,0,0,0.6) 100%)",
              }}
            />
            <span
              className="absolute left-3.5 top-3.5 z-[2] rounded-full bg-white/95 px-2.5 py-1 text-[10px] font-bold tracking-[0.04em] text-[#D88A9E]"
            >
              NEW
            </span>
            <div className="absolute bottom-[18px] left-[18px] right-[18px] z-[2] text-white">
              <div className="mb-1.5 text-[10px] font-bold uppercase tracking-[0.12em] opacity-90">
                SUNDAY MORNING
              </div>
              <div className="mb-1 text-[18px] font-bold leading-[1.3] tracking-[-0.02em]">
                아침 리포머 클래스가
                <br />
                새로 열렸어요
              </div>
              <div className="text-[12px] opacity-80">매주 일 09:00 시작</div>
            </div>
          </Link>
        </section>

        {/* 학원 안내 */}
        <section className="rounded-[18px] border border-[#F0EBE8] bg-white p-4">
          <h3 className="mb-2 text-[14px] font-bold text-[#2A2A2C]">학원 안내</h3>
          <div className="flex flex-col gap-1 text-[13px] text-[#6B6B6B]">
            <p>운영시간: 평일 09:00 ~ 21:00 / 토 09:00 ~ 17:00</p>
            <p>휴무일: 일요일, 공휴일</p>
            <p>문의: 02-1234-5678</p>
          </div>
        </section>
      </main>

      <MobileTabBar />

      <SearchSheet open={searchOpen} onClose={() => setSearchOpen(false)} />
    </div>
  );
}
