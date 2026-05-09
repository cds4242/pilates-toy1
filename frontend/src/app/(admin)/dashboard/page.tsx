"use client";

import { useEffect, useState } from "react";
import { adminApi } from "@/lib/api/admin";
import type { DashboardData } from "@/lib/types/domain";
import { KpiCard } from "@/components/design/KpiCard";
import { formatTime } from "@/lib/utils/format";
import { AlertTriangle, Clock, User } from "lucide-react";

export default function AdminDashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try { setData(await adminApi.getDashboard()); } catch { /* empty */ }
      finally { setLoading(false); }
    }
    load();
  }, []);

  return (
    <div>
      <h1 className="text-[26px] font-bold text-[var(--color-text-title)] mb-6">대시보드</h1>

      {/* KPI */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <KpiCard label="오늘 수업" value={loading ? "—" : `${data?.todayClasses.count ?? 0}건`} />
        <KpiCard label="오늘 예약" value={loading ? "—" : `${data?.todayClasses.schedules.reduce((s, c) => s + c.reservedCount, 0) ?? 0}명`} />
        <KpiCard label="이번 주 매출" value={loading ? "—" : `${Number(data?.thisWeekRevenue.total ?? 0).toLocaleString()}원`} />
        <KpiCard label="만료 임박" value={loading ? "—" : `${data?.expiringMemberships.length ?? 0}명`} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 오늘의 수업 */}
        <div className="rounded-[18px] border border-[var(--color-border)] bg-white p-5">
          <h2 className="text-[18px] font-bold text-[var(--color-text-title)] mb-4">오늘의 수업</h2>
          {!data || data.todayClasses.schedules.length === 0 ? (
            <p className="text-[15px] text-[var(--color-text-sub)] py-4">예정된 수업이 없습니다</p>
          ) : (
            <div className="flex flex-col">
              {data.todayClasses.schedules.map((s, i) => (
                <div key={i} className="flex items-center justify-between py-3 border-b border-[var(--color-border)] last:border-0">
                  <div>
                    <p className="text-[15px] font-semibold text-[var(--color-text-title)]">{formatTime(s.time)} {s.className}</p>
                    <p className="text-[13px] text-[var(--color-text-body)]">{s.instructor}</p>
                  </div>
                  <span className="text-[13px] font-semibold text-[var(--color-text-sub)]">{s.reservedCount}/{s.capacity}명</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 알림 */}
        <div className="rounded-[18px] border border-[var(--color-border)] bg-white p-5">
          <h2 className="text-[18px] font-bold text-[var(--color-text-title)] mb-4">알림</h2>
          <div className="flex flex-col gap-3">
            {data?.alerts.noShowMembers.map((m) => (
              <div key={m.memberId} className="flex items-center gap-3 rounded-[8px] bg-[#FDECEA] px-4 py-3">
                <AlertTriangle className="h-4 w-4 text-[var(--color-error)] shrink-0" />
                <span className="text-[13px] text-[var(--color-text-title)]">{m.memberName} — 노쇼 {m.noShowCount}회</span>
              </div>
            ))}
            {data?.expiringMemberships.slice(0, 3).map((m) => (
              <div key={m.memberId} className="flex items-center gap-3 rounded-[8px] bg-[#FFF3E0] px-4 py-3">
                <Clock className="h-4 w-4 text-[var(--color-warning)] shrink-0" />
                <span className="text-[13px] text-[var(--color-text-title)]">{m.memberName} — D-{m.daysLeft}</span>
              </div>
            ))}
            {data?.alerts.lowMembershipMembers.slice(0, 3).map((m) => (
              <div key={m.memberId} className="flex items-center gap-3 rounded-[8px] bg-[#FFF3E0] px-4 py-3">
                <User className="h-4 w-4 text-[var(--color-warning)] shrink-0" />
                <span className="text-[13px] text-[var(--color-text-title)]">{m.memberName} — 잔여 {m.remainingCount}회</span>
              </div>
            ))}
            {(!data || (data.alerts.noShowMembers.length === 0 && data.expiringMemberships.length === 0 && data.alerts.lowMembershipMembers.length === 0)) && !loading && (
              <p className="text-[15px] text-[var(--color-text-sub)]">알림이 없습니다</p>
            )}
          </div>
        </div>
      </div>

      {/* 매출 추이 */}
      <div className="mt-6 rounded-[18px] border border-[var(--color-border)] bg-white p-5">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-[18px] font-bold text-[var(--color-text-title)]">이번 주 매출 추이</h2>
          <span className="text-[13px] text-[var(--color-text-sub)]">총 {Number(data?.thisWeekRevenue.total ?? 0).toLocaleString()}원</span>
        </div>
        {data?.thisWeekRevenue.breakdown && data.thisWeekRevenue.breakdown.length > 0 ? (() => {
          const breakdown = data.thisWeekRevenue.breakdown;
          const max = Math.max(...breakdown.map((b) => Number(b.amount)), 1);
          const chartHeight = 160;
          return (
            <div className="flex items-end gap-3" style={{ height: chartHeight }}>
              {breakdown.map((d, i) => {
                const amt = Number(d.amount);
                const barH = Math.max(4, (amt / max) * (chartHeight - 30));
                return (
                  <div key={i} className="flex-1 flex flex-col items-center justify-end gap-1 group relative" style={{ height: chartHeight }}>
                    {/* 호버 툴팁 */}
                    <div className="absolute -top-1 opacity-0 group-hover:opacity-100 transition-opacity bg-text-title text-white text-[11px] px-2 py-1 rounded-[6px] whitespace-nowrap z-10">
                      {amt.toLocaleString()}원
                    </div>
                    {/* 금액 라벨 */}
                    {amt > 0 && <span className="text-[10px] font-semibold text-[var(--color-text-body)] mb-0.5">{amt >= 10000 ? `${Math.round(amt/10000)}만` : amt.toLocaleString()}</span>}
                    {/* 바 */}
                    <div
                      className="w-full rounded-t-[4px] bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] transition-colors cursor-pointer"
                      style={{ height: barH }}
                    />
                    {/* 날짜 */}
                    <span className="text-[10px] text-[var(--color-text-sub)] mt-1">{d.date.slice(5)}</span>
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
