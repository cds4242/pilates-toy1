"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { adminApi } from "@/lib/api/admin";
import { api } from "@/lib/api/client";
import type { DashboardData } from "@/lib/types/domain";
import { formatTime } from "@/lib/utils/format";
import { AlertTriangle, Banknote, Calendar, ChevronLeft, ChevronRight, Clock, User, Users } from "lucide-react";
import { usePageTitle } from "@/lib/hooks/use-page-title";
import { useAuth } from "@/lib/hooks/use-auth";

interface RevenueData {
  total: number;
  breakdown: { date: string; amount: number }[];
}

export default function AdminDashboardPage() {
  usePageTitle("대시보드");
  const router = useRouter();
  const { user } = useAuth();
  const revenueRef = useRef<HTMLDivElement>(null);
  const todayClassRef = useRef<HTMLDivElement>(null);

  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  // 매출 추이 네비게이션
  const [revPeriod, setRevPeriod] = useState<"week" | "month">("week");
  const [revOffset, setRevOffset] = useState(0);
  const [revenue, setRevenue] = useState<RevenueData | null>(null);
  const [revLoading, setRevLoading] = useState(false);

  useEffect(() => {
    async function load() {
      try { setData(await adminApi.getDashboard()); } catch { /* empty */ }
      finally { setLoading(false); }
    }
    load();
  }, []);

  // 매출 추이 API 호출
  useEffect(() => {
    async function loadRevenue() {
      setRevLoading(true);
      try {
        const res = await api<RevenueData>("get", "/api/admin/dashboard/revenue", { period: revPeriod, offset: revOffset });
        setRevenue(res);
      } catch { /* empty */ }
      finally { setRevLoading(false); }
    }
    loadRevenue();
  }, [revPeriod, revOffset]);

  const revLabel = () => {
    if (!revenue?.breakdown?.length) return "";
    const first = revenue.breakdown[0].date;
    const last = revenue.breakdown[revenue.breakdown.length - 1].date;
    return `${first.slice(5)} ~ ${last.slice(5)}`;
  };

  return (
    <div>
      <div className="hero-gradient-soft rounded-[18px] p-6 mb-6 animate-fade-in hidden md:block">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-[24px] font-bold text-[var(--color-text-title)]">안녕하세요, {user?.name || "관리자"}님</h1>
            <p className="text-[14px] text-[var(--color-text-body)] mt-1">오늘의 학원 현황을 한눈에 확인하세요</p>
          </div>
          <div className="text-[14px] text-[var(--color-text-sub)]">
            {new Date().toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric", weekday: "long" })}
          </div>
        </div>
      </div>

      {/* 시작 가이드 — 데이터가 적을 때 표시 */}
      {!loading && data && data.todayClasses.count === 0 && (
        <div className="rounded-[18px] hero-gradient-soft card-elevated-md p-5 mb-6">
          <h2 className="text-[16px] font-bold text-[var(--color-text-title)] mb-3">처음이신가요? 시작 가이드</h2>
          <div className="flex flex-col gap-2">
            <button onClick={() => router.push("/instructors")} className="flex items-center gap-3 text-left rounded-[8px] bg-white px-4 py-3 border border-[var(--color-border)] hover:border-[var(--color-pilates)] transition-colors">
              <span className="w-6 h-6 rounded-full bg-[var(--color-pilates)] text-white text-[13px] font-bold flex items-center justify-center shrink-0">1</span>
              <div><p className="text-[14px] font-semibold text-[var(--color-text-title)]">강사 등록하기</p><p className="text-[12px] text-[var(--color-text-sub)]">수업을 진행할 강사를 먼저 등록하세요</p></div>
            </button>
            <button onClick={() => router.push("/classes")} className="flex items-center gap-3 text-left rounded-[8px] bg-white px-4 py-3 border border-[var(--color-border)] hover:border-[var(--color-pilates)] transition-colors">
              <span className="w-6 h-6 rounded-full bg-[var(--color-pilates)] text-white text-[13px] font-bold flex items-center justify-center shrink-0">2</span>
              <div><p className="text-[14px] font-semibold text-[var(--color-text-title)]">시간표 설정하기</p><p className="text-[12px] text-[var(--color-text-sub)]">수업 일정을 등록하고 자동 생성을 활용하세요</p></div>
            </button>
            <button onClick={() => router.push("/members")} className="flex items-center gap-3 text-left rounded-[8px] bg-white px-4 py-3 border border-[var(--color-border)] hover:border-[var(--color-pilates)] transition-colors">
              <span className="w-6 h-6 rounded-full bg-[var(--color-pilates)] text-white text-[13px] font-bold flex items-center justify-center shrink-0">3</span>
              <div><p className="text-[14px] font-semibold text-[var(--color-text-title)]">회원 등록하기</p><p className="text-[12px] text-[var(--color-text-sub)]">회원을 등록하고 수강권을 발급하세요</p></div>
            </button>
          </div>
        </div>
      )}

      {/* KPI */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[
          { icon: Calendar, label: "오늘 수업", value: loading ? "—" : `${data?.todayClasses.count ?? 0}건`, sub: "클릭하여 시간표 보기", onClick: () => router.push("/classes") },
          { icon: Users, label: "오늘 예약", value: loading ? "—" : `${data?.todayClasses.schedules.reduce((s, c) => s + c.reservedCount, 0) ?? 0}명`, sub: "클릭하여 회원 보기", onClick: () => router.push("/members") },
          { icon: Banknote, label: "이번 주 매출", value: loading ? "—" : `${Number(data?.thisWeekRevenue.total ?? 0).toLocaleString()}원`, sub: "아래 차트에서 상세 확인", onClick: () => revenueRef.current?.scrollIntoView({ behavior: "smooth" }) },
          { icon: Clock, label: "만료 임박", value: loading ? "—" : `${data?.expiringMemberships.length ?? 0}명`, sub: "클릭하여 회원 보기", onClick: () => router.push("/members") },
        ].map(({ icon: Icon, label, value, sub, onClick }) => (
          <div key={label} onClick={onClick} className="cursor-pointer rounded-[18px] kpi-card p-5 card-hover">
            <div className="flex items-center gap-2 mb-2">
              <Icon className="h-5 w-5 text-pilates-dark" />
              <p className="text-[13px] font-semibold text-[var(--color-text-body)]">{label}</p>
            </div>
            <p className="text-[28px] font-bold text-[var(--color-text-title)]">{value}</p>
            <p className="text-[11px] text-[var(--color-text-sub)] mt-1">{sub}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 오늘의 수업 */}
        <div ref={todayClassRef} className="section-card p-5 animate-fade-in">
          <h2 className="text-[18px] font-bold text-[var(--color-text-title)] mb-4">오늘의 수업</h2>
          {!data || data.todayClasses.schedules.length === 0 ? (
            <p className="text-[15px] text-[var(--color-text-sub)] py-4">예정된 수업이 없습니다</p>
          ) : (
            <div className="flex flex-col">
              {data.todayClasses.schedules.map((s, i) => (
                <div key={i} onClick={() => router.push("/classes")} className="flex items-center justify-between py-3 border-b border-[var(--color-border)] last:border-0 cursor-pointer hover:bg-[var(--color-bg-section)] rounded-[8px] px-2 -mx-2 transition-colors">
                  <div>
                    <p className="text-[15px] font-semibold text-[var(--color-text-title)]">{formatTime(s.time)} {s.className}</p>
                    <p className="text-[13px] text-[var(--color-text-body)]">{s.instructor}</p>
                  </div>
                  {s.reservedCount === 0 ? (
                    <span className="text-[12px] font-semibold text-white bg-[var(--color-error)] rounded-full px-2.5 py-0.5">예약 없음</span>
                  ) : s.reservedCount >= s.capacity ? (
                    <span className="text-[12px] font-semibold text-white bg-[#4CAF50] rounded-full px-2.5 py-0.5">마감</span>
                  ) : (
                    <span className="text-[13px] font-semibold text-[var(--color-text-sub)]">{s.reservedCount}/{s.capacity}명</span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 알림 */}
        <div className="section-card p-5 animate-fade-in">
          <h2 className="text-[18px] font-bold text-[var(--color-text-title)] mb-4">알림</h2>
          <div className="flex flex-col gap-3">
            {data?.alerts.noShowMembers.map((m) => (
              <div key={m.memberId} onClick={() => router.push("/members")} className="flex items-center gap-3 rounded-[8px] bg-[#FDECEA] px-4 py-3 cursor-pointer hover:opacity-80 transition-opacity">
                <AlertTriangle className="h-4 w-4 text-[var(--color-error)] shrink-0" />
                <span className="text-[13px] text-[var(--color-text-title)]">{m.memberName} — 노쇼 {m.noShowCount}회</span>
              </div>
            ))}
            {data?.expiringMemberships.slice(0, 3).map((m) => (
              <div key={m.memberId} onClick={() => router.push("/members")} className="flex items-center gap-3 rounded-[8px] bg-[#FFF3E0] px-4 py-3 cursor-pointer hover:opacity-80 transition-opacity">
                <Clock className="h-4 w-4 text-[var(--color-warning)] shrink-0" />
                <span className="text-[13px] text-[var(--color-text-title)]">{m.memberName} — 정기권 만료 D-{m.daysLeft}</span>
              </div>
            ))}
            {data?.alerts.lowMembershipMembers.slice(0, 3).map((m) => (
              <div key={m.memberId} onClick={() => router.push("/members")} className="flex items-center gap-3 rounded-[8px] bg-[#EBF5FB] px-4 py-3 cursor-pointer hover:opacity-80 transition-opacity">
                <User className="h-4 w-4 text-[#2563EB] shrink-0" />
                <span className="text-[13px] text-[var(--color-text-title)]">{m.memberName} — 수강권 잔여 {m.remainingCount}회</span>
              </div>
            ))}
            {(!data || (data.alerts.noShowMembers.length === 0 && data.expiringMemberships.length === 0 && data.alerts.lowMembershipMembers.length === 0)) && !loading && (
              <p className="text-[15px] text-[var(--color-text-sub)]">알림이 없습니다</p>
            )}
          </div>
        </div>
      </div>

      {/* 매출 추이 */}
      <div ref={revenueRef} className="mt-6 section-card p-5 animate-fade-in">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div className="flex items-center gap-3">
            <h2 className="text-[18px] font-bold text-[var(--color-text-title)]">매출 추이</h2>
            <div className="flex rounded-[8px] border border-[var(--color-border)] overflow-hidden">
              <button
                onClick={() => { setRevPeriod("week"); setRevOffset(0); }}
                className={`px-3 py-1.5 text-[12px] font-semibold transition-colors ${revPeriod === "week" ? "bg-[var(--color-pilates)] text-[var(--color-text-title)]" : "text-[var(--color-text-sub)] hover:bg-[var(--color-bg-section)]"}`}
              >주간</button>
              <button
                onClick={() => { setRevPeriod("month"); setRevOffset(0); }}
                className={`px-3 py-1.5 text-[12px] font-semibold transition-colors ${revPeriod === "month" ? "bg-[var(--color-pilates)] text-[var(--color-text-title)]" : "text-[var(--color-text-sub)] hover:bg-[var(--color-bg-section)]"}`}
              >월간</button>
              <button
                onClick={() => { setRevPeriod("month"); setRevOffset(-2); }}
                className={`px-3 py-1.5 text-[12px] font-semibold transition-colors ${revPeriod === "month" && revOffset === -2 ? "bg-[var(--color-pilates)] text-[var(--color-text-title)]" : "text-[var(--color-text-sub)] hover:bg-[var(--color-bg-section)]"}`}
              >3개월</button>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => setRevOffset(revOffset - 1)} className="p-1 rounded hover:bg-[var(--color-bg-section)]">
              <ChevronLeft className="h-5 w-5 text-[var(--color-text-body)]" />
            </button>
            <span className="text-[13px] text-[var(--color-text-body)] min-w-[100px] text-center">{revLabel()}</span>
            <button onClick={() => setRevOffset(revOffset + 1)} disabled={revOffset >= 0} className="p-1 rounded hover:bg-[var(--color-bg-section)] disabled:opacity-30">
              <ChevronRight className="h-5 w-5 text-[var(--color-text-body)]" />
            </button>
            {revOffset !== 0 && (
              <button onClick={() => setRevOffset(0)} className="px-2 py-1 rounded-[6px] text-[12px] font-semibold text-[var(--color-pilates-dark)] bg-[var(--color-pilates-light)] hover:bg-[var(--color-pilates)] transition-colors">오늘</button>
            )}
            <span className="text-[13px] text-[var(--color-text-sub)] ml-2">총 {Number(revenue?.total ?? 0).toLocaleString()}원</span>
          </div>
        </div>
        {revLoading ? (
          <div className="h-[160px] flex items-center justify-center"><span className="text-[var(--color-text-sub)] text-[13px]">로딩 중...</span></div>
        ) : revenue?.breakdown && revenue.breakdown.length > 0 ? (() => {
          const breakdown = revenue.breakdown;
          const max = Math.max(...breakdown.map((b) => Number(b.amount)), 1);
          const chartHeight = 160;
          return (
            <div className="flex items-end gap-1" style={{ height: chartHeight }}>
              {breakdown.map((d, i) => {
                const amt = Number(d.amount);
                const barH = Math.max(4, (amt / max) * (chartHeight - 30));
                return (
                  <div key={i} className="flex-1 flex flex-col items-center justify-end gap-1 group relative" style={{ height: chartHeight }}>
                    <div className="absolute -top-1 opacity-0 group-hover:opacity-100 transition-opacity bg-text-title text-white text-[11px] px-2 py-1 rounded-[6px] whitespace-nowrap z-10">
                      {amt.toLocaleString()}원
                    </div>
                    <span className="text-[10px] font-semibold text-[var(--color-text-body)] mb-0.5">{amt === 0 ? "0원" : amt >= 10000 ? `${Math.round(amt/10000)}만` : amt.toLocaleString()}</span>
                    <div
                      className="w-full rounded-t-[4px] bg-gradient-to-t from-[var(--color-pilates-dark)] to-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] transition-colors cursor-pointer"
                      style={{ height: barH, maxWidth: 40 }}
                    />
                    <span className="text-[10px] text-[var(--color-text-sub)] mt-1">{d.date.slice(5)} {["일","월","화","수","목","금","토"][new Date(d.date).getDay()]}</span>
                  </div>
                );
              })}
            </div>
          );
        })() : (
          <p className="text-[15px] text-[var(--color-text-sub)] py-4">매출 데이터가 없습니다</p>
        )}
      </div>
    </div>
  );
}
