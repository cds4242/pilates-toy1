"use client";

import { useEffect, useState } from "react";
import { memberApi } from "@/lib/api/member";
import { api } from "@/lib/api/client";
import type { Membership } from "@/lib/types/domain";
import { PassCard } from "@/components/design/PassCard";
import { StatusBadge } from "@/components/design/StatusBadge";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { toast } from "sonner";
import { HelpTip } from "@/components/design/HelpTip";

interface MembershipPass {
  id: number;
  name: string;
  price: number;
  totalCount: number | null;
  validityDays: number;
  unlimited?: boolean;
  category?: string;
  description?: string;
  lessonTypeNames?: string[];
  lessonTypes?: { id: number; name: string }[];
}

export default function MembershipPage() {
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [loading, setLoading] = useState(true);
  const [showPasses, setShowPasses] = useState(true);
  const [passes, setPasses] = useState<MembershipPass[]>([]);
  const [passesLoading, setPassesLoading] = useState(false);
  const [purchaseTarget, setPurchaseTarget] = useState<MembershipPass | null>(null);
  const [purchasing, setPurchasing] = useState(false);

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
      <header className="sticky top-0 z-50 bg-gradient-to-r from-white to-[#FFF5F7] px-6 py-4 border-b border-[var(--color-border)]">
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">수강권</h1>
      </header>
      <main className="p-6 flex flex-col gap-6">
        <div>
          <h2 className="text-[20px] font-bold text-[var(--color-text-title)] mb-3">활성 수강권 <HelpTip text="현재 사용 가능한 수강권입니다. 잔여 횟수와 만료일을 확인하세요. 만료 전에 수업을 예약하여 사용해주세요." /></h2>
          {loading ? (
            <div className="rounded-[18px] bg-[var(--color-pilates-light)] p-5 animate-pulse h-32" />
          ) : active ? (
            <div className="card-elevated-md"><PassCard name={active.passName} remaining={active.remainingCount} total={active.totalCount} unlimited={active.unlimited} endDate={active.endDate} /></div>
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
        <button onClick={handleTogglePasses} className={showPasses
          ? "rounded-[8px] border border-[var(--color-border)] py-3 text-[15px] font-semibold text-[var(--color-text-body)] w-full hover:bg-[var(--color-bg-section)] transition-colors"
          : "btn-primary rounded-[8px] py-4 text-[16px] font-semibold w-full"
        }>
          {showPasses ? "접기" : "수강권 구매하기"}
        </button>
        {showPasses && (
          <div>
            <h2 className="text-[20px] font-bold text-[var(--color-text-title)] mb-3">수강권 상품 <HelpTip text="구매 가능한 수강권 목록입니다. 카드를 클릭하면 결제 화면으로 이동합니다." /></h2>
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
                    : "bg-white card-elevated card-hover";
                  const textCls = isUnlimited || isPersonal10 ? "text-white" : "text-[var(--color-text-title)]";
                  const subCls = isUnlimited || isPersonal10 ? "text-white/80" : "text-[var(--color-text-sub)]";
                  const priceCls = isUnlimited || isPersonal10 ? "text-white" : "text-[var(--color-pilates-dark)]";
                  return (
                    <div
                      key={p.id}
                      onClick={() => setPurchaseTarget(p)}
                      className={`relative rounded-[18px] border border-[var(--color-border)] p-5 cursor-pointer hover:shadow-md transition-all ${cardCls}`}
                    >
                      {isUnlimited && (
                        <span className="absolute bottom-3 right-3 bg-white text-[var(--color-pilates-dark)] text-[11px] font-bold px-2 py-0.5 rounded-full shadow-sm">BEST</span>
                      )}
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <p className={`text-[16px] font-bold ${textCls}`}>{p.name}</p>
                          {p.category && (
                            <span className="text-[11px] bg-[var(--color-bg-section)] px-2 py-0.5 rounded-full text-[var(--color-text-sub)]">
                              {p.category === "PERSONAL" ? "개인" : p.category === "GROUP" ? "그룹" : p.category === "UNLIMITED" ? "무제한" : p.category}
                            </span>
                          )}
                        </div>
                        <p className={`text-[16px] font-bold ${priceCls}`}>{p.price.toLocaleString()}원</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className={`text-[13px] ${isUnlimited || isPersonal10 ? "text-white/90" : "text-[var(--color-text-body)]"}`}>{p.totalCount ? `${p.totalCount}회` : "무제한"}</span>
                        <span className={`text-[13px] ${subCls}`}>유효기간 {p.validityDays}일</span>
                      </div>
                      {p.lessonTypeNames && p.lessonTypeNames.length > 0 && (
                        <p className={`text-[11px] mt-1 ${subCls}`}>{p.lessonTypeNames.join(", ")}</p>
                      )}
                      {p.description && (
                        <p className={`text-[12px] mt-1 ${subCls}`}>{p.description}</p>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </main>
      <MobileTabBar />

      {/* 모의 결제 모달 */}
      {purchaseTarget && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={() => setPurchaseTarget(null)}>
          <div className="bg-white rounded-[18px] w-full max-w-[400px] p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-[18px] font-bold text-[var(--color-text-title)] mb-4">수강권 구매</h3>
            <div className="rounded-[12px] bg-[var(--color-bg-section)] p-4 mb-4">
              <p className="text-[16px] font-semibold text-[var(--color-text-title)]">{purchaseTarget.name}</p>
              <div className="flex justify-between mt-2 text-[14px]">
                <span className="text-[var(--color-text-sub)]">{purchaseTarget.totalCount ? `${purchaseTarget.totalCount}회` : "무제한"} · {purchaseTarget.validityDays}일</span>
                <span className="font-bold text-[var(--color-pilates-dark)]">{purchaseTarget.price.toLocaleString()}원</span>
              </div>
            </div>
            <div className="rounded-[12px] border border-[var(--color-border)] p-4 mb-6">
              <p className="text-[13px] font-semibold text-[var(--color-text-title)] mb-2">결제 수단</p>
              <div className="flex gap-2">
                <button className="flex-1 rounded-[8px] bg-[var(--color-pilates-light)] border-2 border-[var(--color-pilates)] py-2.5 text-[13px] font-semibold text-[var(--color-text-title)]">카드 결제</button>
                <button className="flex-1 rounded-[8px] bg-[var(--color-bg-section)] border border-[var(--color-border)] py-2.5 text-[13px] text-[var(--color-text-sub)]">계좌이체</button>
              </div>
              <p className="text-[11px] text-[var(--color-text-sub)] mt-2">* 결제 후 수강권이 즉시 발급됩니다</p>
            </div>
            <div className="flex gap-3">
              <button onClick={() => setPurchaseTarget(null)} className="flex-1 rounded-[8px] border border-[var(--color-border)] py-3 text-[15px] font-semibold text-[var(--color-text-body)]">취소</button>
              <button
                onClick={async () => {
                  setPurchasing(true);
                  try {
                    // 실제 수강권 발급 API 호출
                    await api("post", "/api/members/me/memberships/purchase", {
                      totalCount: purchaseTarget.totalCount ?? 0,
                      price: purchaseTarget.price,
                      validityDays: purchaseTarget.validityDays,
                      unlimited: !purchaseTarget.totalCount,
                      lessonTypeIds: purchaseTarget.lessonTypes?.map((lt: { id: number }) => lt.id) || [],
                      membershipPassId: purchaseTarget.id,
                    });
                    toast.success(`${purchaseTarget.name} 구매가 완료되었습니다!`);
                    setPurchaseTarget(null);
                    // 멤버십 목록 새로고침
                    const ms = await memberApi.getMemberships();
                    setMemberships(ms);
                  } catch (err: unknown) {
                    toast.error(err instanceof Error ? err.message : "구매 처리에 실패했습니다.");
                  } finally {
                    setPurchasing(false);
                  }
                }}
                disabled={purchasing}
                className="flex-1 rounded-[8px] bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] py-3 text-[15px] font-semibold text-[var(--color-text-title)] disabled:opacity-60"
              >
                {purchasing ? "결제 중..." : `${purchaseTarget.price.toLocaleString()}원 결제`}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
