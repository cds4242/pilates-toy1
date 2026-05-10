"use client";

import { useEffect, useState } from "react";
import { memberApi } from "@/lib/api/member";
import { api } from "@/lib/api/client";
import type { Membership } from "@/lib/types/domain";
import { PassCard } from "@/components/design/PassCard";
import { StatusBadge } from "@/components/design/StatusBadge";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { toast } from "sonner";

interface MembershipPass {
  id: number;
  name: string;
  price: number;
  totalCount: number | null;
  validityDays: number;
}

export default function MembershipPage() {
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [loading, setLoading] = useState(true);
  const [showPasses, setShowPasses] = useState(true);
  const [passes, setPasses] = useState<MembershipPass[]>([]);
  const [passesLoading, setPassesLoading] = useState(false);

  useEffect(() => {
    async function load() {
      try { setMemberships(await memberApi.getMemberships()); } catch { /* empty */ }
      finally { setLoading(false); }
      // 초기 상태가 true이므로 수강권 상품도 즉시 로드
      setPassesLoading(true);
      try {
        setPasses(await api<MembershipPass[]>("get", "/api/membership-passes"));
      } catch { /* empty */ }
      finally { setPassesLoading(false); }
    }
    load();
  }, []);

  const handleTogglePasses = async () => {
    if (showPasses) {
      setShowPasses(false);
      return;
    }
    setShowPasses(true);
    if (passes.length > 0) return;
    setPassesLoading(true);
    try {
      setPasses(await api<MembershipPass[]>("get", "/api/membership-passes"));
    } catch {
      toast.error("수강권 목록 조회 실패");
    } finally {
      setPassesLoading(false);
    }
  };

  const active = memberships.find((m) => m.status === "ACTIVE");
  const past = memberships.filter((m) => m.status !== "ACTIVE");

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white pb-20">
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
        <button onClick={handleTogglePasses} className="w-full bg-pilates hover:bg-pilates-dark text-text-title rounded-[8px] py-4 text-[16px] font-semibold transition-all">
          {showPasses ? "접기" : "수강권 구매하기"}
        </button>
        {showPasses && (
          <div>
            <h2 className="text-[20px] font-bold text-[var(--color-text-title)] mb-3">수강권 상품</h2>
            {passesLoading ? (
              <div className="grid grid-cols-1 gap-3">
                {[1, 2].map((n) => (
                  <div key={n} className="rounded-[18px] bg-[var(--color-pilates-light)] p-5 animate-pulse h-28" />
                ))}
              </div>
            ) : passes.length === 0 ? (
              <div className="rounded-[18px] bg-[var(--color-bg-section)] p-5 text-center text-[15px] text-[var(--color-text-sub)]">등록된 수강권 상품이 없습니다</div>
            ) : (
              <div className="grid grid-cols-1 gap-3">
                {passes.map((p) => {
                  const isUnlimited = !p.totalCount;
                  const isPersonal10 = p.totalCount === 10 && p.name.includes("개인");
                  const cardCls = isUnlimited
                    ? "card-premium-gold"
                    : isPersonal10
                    ? "card-premium"
                    : "bg-white card-elevated";
                  const textCls = isUnlimited || isPersonal10 ? "text-white" : "text-[var(--color-text-title)]";
                  const subCls = isUnlimited || isPersonal10 ? "text-white/80" : "text-[var(--color-text-sub)]";
                  const priceCls = isUnlimited || isPersonal10 ? "text-white" : "text-[var(--color-pilates-dark)]";
                  return (
                    <div
                      key={p.id}
                      onClick={() => toast.info("온라인 결제는 준비 중입니다. 스튜디오 방문 시 수강권 구매가 가능합니다.")}
                      className={`relative rounded-[18px] border border-[var(--color-border)] p-5 cursor-pointer hover:shadow-md transition-all ${cardCls}`}
                    >
                      {isUnlimited && (
                        <span className="absolute top-3 right-3 bg-white text-[var(--color-pilates-dark)] text-[11px] font-bold px-2 py-0.5 rounded-full shadow-sm">BEST</span>
                      )}
                      <div className="flex items-center justify-between mb-2">
                        <p className={`text-[16px] font-bold ${textCls}`}>{p.name}</p>
                        <p className={`text-[16px] font-bold ${priceCls}`}>{p.price.toLocaleString()}원</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className={`text-[13px] ${isUnlimited || isPersonal10 ? "text-white/90" : "text-[var(--color-text-body)]"}`}>{p.totalCount ? `${p.totalCount}회` : "무제한"}</span>
                        <span className={`text-[13px] ${subCls}`}>유효기간 {p.validityDays}일</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </main>
      <MobileTabBar />
    </div>
  );
}
