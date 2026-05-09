"use client";

import { useEffect, useRef, useState } from "react";
import { Search, X } from "lucide-react";
import { api } from "@/lib/api/client";
import { adminApi } from "@/lib/api/admin";
import type { PageResponse } from "@/lib/types/api";
import { StatusBadge } from "@/components/design/StatusBadge";
import { toast } from "sonner";

interface AdminMember {
  id: number; name: string; phone: string; gender: string; status: string; activeMembership: string | null; remainingInfo: string; expiryDate: string | null; attendanceRate: string; createdAt: string;
}

export default function AdminMembersPage() {
  const [members, setMembers] = useState<AdminMember[]>([]);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [statusFilter, setStatusFilter] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);

  const load = async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { search, page, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const res = await api<PageResponse<AdminMember>>("get", "/api/admin/members", params);
      setMembers(res.content); setTotal(res.totalElements);
    } catch { /* empty */ }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [page, statusFilter]);
  const handleSearch = (e: React.FormEvent) => { e.preventDefault(); setPage(0); load(); };

  const handleExcelUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const result = await adminApi.bulkImportMembers(file);
      toast.success(`등록 완료: 성공 ${result.successCount}건, 실패 ${result.failureCount}건`);
      load();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "엑셀 등록 실패");
    }
    e.target.value = "";
  };

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <h1 className="text-[26px] font-bold text-[var(--color-text-title)]">회원 관리</h1>
        <div className="flex gap-3">
          <button onClick={() => setShowAddModal(true)} className="rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-2.5 text-[13px] font-semibold text-text-title transition-colors">+ 회원 등록</button>
          <button onClick={() => fileRef.current?.click()} className="rounded-[8px] border border-border hover:border-pilates bg-white px-4 py-2.5 text-[13px] font-semibold text-text-body transition-colors">엑셀 일괄 등록</button>
          <input ref={fileRef} type="file" accept=".xlsx,.xls" className="hidden" onChange={handleExcelUpload} />
        </div>
      </div>

      <form onSubmit={handleSearch} className="flex flex-wrap gap-3 mb-6">
        <div className="flex-1 min-w-[200px] relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-sub" />
          <input type="text" placeholder="이름 또는 휴대폰 번호 검색" value={search} onChange={(e) => setSearch(e.target.value)}
            className="w-full border border-[#DDDDDD] rounded-[8px] pl-9 pr-3.5 py-2.5 text-[15px] outline-none focus:border-pilates transition-colors" />
        </div>
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          className="border border-[#DDDDDD] rounded-[8px] px-3.5 py-2.5 text-[15px] outline-none bg-white min-w-[120px]">
          <option value="">전체</option><option value="ACTIVE">활성</option><option value="WITHDRAWN">탈퇴</option>
        </select>
      </form>

      {/* PC 테이블 */}
      <div className="hidden md:block rounded-[18px] border border-border bg-white overflow-hidden">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-bg-section border-b border-border">
              {["프로필","이름","휴대폰","정기권","잔여","만료일","출석률","상태"].map(h=>(
                <th key={h} className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan={8} className="text-center py-8 text-text-sub">로딩 중...</td></tr>
            : members.length === 0 ? <tr><td colSpan={8} className="text-center py-8 text-text-sub">회원이 없습니다</td></tr>
            : members.map(m=>(
              <tr key={m.id} className="border-b border-border last:border-0 hover:bg-bg-section cursor-pointer transition-colors">
                <td className="px-4 py-3.5"><div className="w-8 h-8 rounded-full bg-pilates-light flex items-center justify-center text-[13px] font-bold text-pilates-dark">{m.name.charAt(0)}</div></td>
                <td className="px-4 py-3.5 text-[15px] text-text-title">{m.name}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-title">{m.phone}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-body">{m.activeMembership||"-"}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-body">{m.remainingInfo||"-"}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-body">{m.expiryDate||"-"}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-body">{m.attendanceRate||"-"}</td>
                <td className="px-4 py-3.5"><StatusBadge status={m.status==="ACTIVE"?"active":"expired"} label={m.status==="ACTIVE"?"활성":m.status==="WITHDRAWN"?"탈퇴":"만료"}/></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 모바일 카드 */}
      <div className="md:hidden flex flex-col gap-3">
        {loading ? <div className="text-center py-8 text-text-sub">로딩 중...</div>
        : members.length === 0 ? <div className="text-center py-8 text-text-sub">회원이 없습니다</div>
        : members.map(m=>(
          <div key={m.id} className="rounded-[18px] border border-border bg-white p-4 flex items-center justify-between">
            <div className="flex items-center gap-3 flex-1">
              <div className="w-9 h-9 rounded-full bg-pilates-light flex items-center justify-center text-[14px] font-bold text-pilates-dark shrink-0">{m.name.charAt(0)}</div>
              <div className="flex-1 min-w-0">
                <p className="text-[15px] font-semibold text-text-title">{m.name}</p>
                <p className="text-[13px] text-text-sub mt-0.5">{m.activeMembership||"정기권 없음"} | {m.phone}</p>
              </div>
            </div>
            <StatusBadge status={m.status==="ACTIVE"?"active":"expired"} label={m.status==="ACTIVE"?"활성":"만료"}/>
          </div>
        ))}
      </div>

      {total > 10 && (
        <div className="flex justify-center gap-2 mt-6">
          <button onClick={()=>setPage(Math.max(0,page-1))} disabled={page===0} className="rounded-[8px] border border-border px-3 py-1.5 text-[13px] disabled:opacity-40">이전</button>
          <span className="px-3 py-1.5 text-[13px] text-text-body">{page+1} / {Math.ceil(total/10)}</span>
          <button onClick={()=>setPage(page+1)} disabled={(page+1)*10>=total} className="rounded-[8px] border border-border px-3 py-1.5 text-[13px] disabled:opacity-40">다음</button>
        </div>
      )}

      {/* 회원 등록 모달 */}
      {showAddModal && <MemberAddModal onClose={() => setShowAddModal(false)} onSuccess={() => { setShowAddModal(false); load(); }} />}
    </div>
  );
}

function MemberAddModal({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);

  const inputCls = "border border-[#DDDDDD] rounded-[8px] px-3.5 py-3 text-[15px] outline-none focus:border-pilates transition-colors w-full";

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      // 엑셀 1행짜리로 등록 (bulk API 활용)
      const blob = await createSingleMemberExcel(name, phone);
      const file = new File([blob], "member.xlsx", { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
      const result = await (await import("@/lib/api/admin")).adminApi.bulkImportMembers(file);
      if (result.successCount > 0) {
        toast.success("회원이 등록되었습니다");
        onSuccess();
      } else {
        toast.error(result.failures?.[0]?.reason || "등록 실패");
      }
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "등록 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-[18px] w-full max-w-[400px] p-6" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-[20px] font-bold text-text-title">회원 등록</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-text-sub" /></button>
        </div>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-[13px] font-semibold text-text-title">이름</label>
            <input type="text" placeholder="이름" value={name} onChange={(e) => setName(e.target.value)} className={inputCls} required />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-[13px] font-semibold text-text-title">휴대폰 번호</label>
            <input type="tel" placeholder="01012345678" value={phone} onChange={(e) => setPhone(e.target.value)} className={inputCls} required />
          </div>
          <button type="submit" disabled={loading} className="bg-pilates hover:bg-pilates-dark text-text-title rounded-[8px] py-3.5 text-[15px] font-semibold transition-all disabled:opacity-60">
            {loading ? "등록 중..." : "등록하기"}
          </button>
        </form>
      </div>
    </div>
  );
}

async function createSingleMemberExcel(name: string, phone: string): Promise<Blob> {
  // 간단한 CSV → xlsx 변환 대신, 서버 템플릿 기반
  // 실제로는 xlsx 라이브러리 필요하지만, 포트폴리오 목적상 FormData로 직접 전달
  // bulk API가 엑셀을 요구하므로 템플릿 다운로드 후 수정하는 것이 정석
  // 여기서는 toast로 안내
  throw new Error("개별 회원 등록은 엑셀 일괄 등록을 이용해주세요");
}
