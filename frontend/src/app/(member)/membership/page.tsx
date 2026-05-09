"use client";

import { useEffect, useState } from "react";
import { memberApi } from "@/lib/api/member";
import type { Membership } from "@/lib/types/domain";
import { PassCard } from "@/components/design/PassCard";
import { StatusBadge } from "@/components/design/StatusBadge";
import { MobileTabBar } from "@/components/design/MobileTabBar";

export default function MembershipPage() {
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try { setMemberships(await memberApi.getMemberships()); } catch { /* empty */ }
      finally { setLoading(false); }
    }
    load();
  }, []);

  const active = memberships.find((m) => m.status === "ACTIVE");
  const past = memberships.filter((m) => m.status !== "ACTIVE");

  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white pb-20">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 border-b border-[var(--color-border)]">
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">수강권</h1>
      </header>
      <main className="p-6 flex flex-col gap-6">
        <div>
          <h2 className="text-[20px] font-bold text-[var(--color-text-title)] mb-3">활성 수강권</h2>
          {loading ? (
            <div className="rounded-[18px] bg-[var(--color-pilates-light)] p-5 animate-pulse h-32" />
          ) : active ? (
            <PassCard name={active.passName} remaining={active.remainingCount} total={active.totalCount} unlimited={active.unlimited} endDate={active.endDate} />
          ) : (
            <div className="rounded-[18px] bg-[var(--color-bg-section)] p-5 text-center text-[15px] text-[var(--color-text-sub)]">활성 수강권이 없습니다</div>
          )}
        </div>
        {past.length > 0 && (
          <div>
            <h2 className="text-[20px] font-bold text-[var(--color-text-title)] mb-3">지난 수강권</h2>
            <div className="flex flex-col gap-2">
              {past.map((m, i) => (
                <div key={i} className="flex items-center justify-between rounded-[18px] border border-[var(--color-border)] p-4">
                  <div>
                    <p className="text-[15px] font-semibold text-[var(--color-text-title)]">{m.passName}</p>
                    <p className="text-[13px] text-[var(--color-text-sub)]">{m.startDate} ~ {m.endDate}</p>
                  </div>
                  <StatusBadge status={m.status === "EXPIRED" ? "expired" : "active"} label={m.status === "EXPIRED" ? "만료" : m.status === "EXHAUSTED" ? "소진" : m.status} />
                </div>
              ))}
            </div>
          </div>
        )}
        <button className="w-full bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] text-[var(--color-text-title)] rounded-[8px] py-4 text-[16px] font-semibold transition-all">수강권 구매하기</button>
      </main>
      <MobileTabBar />
    </div>
  );
}
