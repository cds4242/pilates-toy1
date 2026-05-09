"use client";

import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import { api } from "@/lib/api/client";
import type { PageResponse } from "@/lib/types/api";
import { StatusBadge } from "@/components/design/StatusBadge";

interface AdminMember {
  id: number; name: string; phone: string; gender: string; status: string; activeMembership: string | null; createdAt: string;
}

export default function AdminMembersPage() {
  const [members, setMembers] = useState<AdminMember[]>([]);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const res = await api<PageResponse<AdminMember>>("get", "/api/admin/members", { search, page, size: 10 });
      setMembers(res.content); setTotal(res.totalElements);
    } catch { /* empty */ }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [page]);

  const handleSearch = (e: React.FormEvent) => { e.preventDefault(); setPage(0); load(); };

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <h1 className="text-[26px] font-bold text-[var(--color-text-title)]">회원 관리</h1>
        <div className="flex gap-3">
          <button className="rounded-[8px] bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] px-4 py-2.5 text-[13px] font-semibold text-[var(--color-text-title)] transition-colors">+ 회원 등록</button>
          <button className="rounded-[8px] border border-[var(--color-border)] hover:border-[var(--color-pilates)] bg-white px-4 py-2.5 text-[13px] font-semibold text-[var(--color-text-body)] transition-colors">엑셀 일괄 등록</button>
        </div>
      </div>

      <form onSubmit={handleSearch} className="flex flex-wrap gap-3 mb-6">
        <div className="flex-1 min-w-[200px] relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[var(--color-text-sub)]" />
          <input type="text" placeholder="이름 또는 휴대폰 번호 검색" value={search} onChange={(e) => setSearch(e.target.value)}
            className="w-full border border-[#DDDDDD] rounded-[8px] pl-9 pr-3.5 py-2.5 text-[15px] outline-none focus:border-[var(--color-pilates)] transition-colors" />
        </div>
        <select className="border border-[#DDDDDD] rounded-[8px] px-3.5 py-2.5 text-[15px] outline-none bg-white min-w-[120px]">
          <option>정기권 상태</option><option>활성</option><option>만료</option>
        </select>
      </form>

      {/* PC 테이블 */}
      <div className="hidden md:block rounded-[18px] border border-[var(--color-border)] bg-white overflow-hidden">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-[var(--color-bg-section)] border-b border-[var(--color-border)]">
              {["프로필","이름","휴대폰","정기권","상태"].map(h=>(
                <th key={h} className="text-left px-4 py-3.5 text-[13px] font-semibold text-[var(--color-text-sub)]">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan={5} className="text-center py-8 text-[var(--color-text-sub)]">로딩 중...</td></tr>
            : members.length === 0 ? <tr><td colSpan={5} className="text-center py-8 text-[var(--color-text-sub)]">회원이 없습니다</td></tr>
            : members.map(m=>(
              <tr key={m.id} className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-bg-section)] cursor-pointer transition-colors">
                <td className="px-4 py-3.5"><div className="w-8 h-8 rounded-full bg-[var(--color-pilates-light)] flex items-center justify-center text-[13px] font-bold text-[var(--color-pilates-dark)]">{m.name.charAt(0)}</div></td>
                <td className="px-4 py-3.5 text-[15px] text-[var(--color-text-title)]">{m.name}</td>
                <td className="px-4 py-3.5 text-[15px] text-[var(--color-text-title)]">{m.phone}</td>
                <td className="px-4 py-3.5 text-[15px] text-[var(--color-text-body)]">{m.activeMembership||"-"}</td>
                <td className="px-4 py-3.5"><StatusBadge status={m.status==="ACTIVE"?"active":"expired"} label={m.status==="ACTIVE"?"활성":m.status==="WITHDRAWN"?"탈퇴":"만료"}/></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 모바일 카드 */}
      <div className="md:hidden flex flex-col gap-3">
        {loading ? <div className="text-center py-8 text-[var(--color-text-sub)]">로딩 중...</div>
        : members.length === 0 ? <div className="text-center py-8 text-[var(--color-text-sub)]">회원이 없습니다</div>
        : members.map(m=>(
          <div key={m.id} className="rounded-[18px] border border-[var(--color-border)] bg-white p-4 flex items-center justify-between">
            <div className="flex items-center gap-3 flex-1">
              <div className="w-9 h-9 rounded-full bg-[var(--color-pilates-light)] flex items-center justify-center text-[14px] font-bold text-[var(--color-pilates-dark)] shrink-0">{m.name.charAt(0)}</div>
              <div className="flex-1 min-w-0">
                <p className="text-[15px] font-semibold text-[var(--color-text-title)]">{m.name}</p>
                <p className="text-[13px] text-[var(--color-text-sub)] mt-0.5">{m.activeMembership||"정기권 없음"} | {m.phone}</p>
              </div>
            </div>
            <StatusBadge status={m.status==="ACTIVE"?"active":"expired"} label={m.status==="ACTIVE"?"활성":"만료"}/>
          </div>
        ))}
      </div>

      {total > 10 && (
        <div className="flex justify-center gap-2 mt-6">
          <button onClick={()=>setPage(Math.max(0,page-1))} disabled={page===0} className="rounded-[8px] border border-[var(--color-border)] px-3 py-1.5 text-[13px] disabled:opacity-40">이전</button>
          <span className="px-3 py-1.5 text-[13px] text-[var(--color-text-body)]">{page+1} / {Math.ceil(total/10)}</span>
          <button onClick={()=>setPage(page+1)} disabled={(page+1)*10>=total} className="rounded-[8px] border border-[var(--color-border)] px-3 py-1.5 text-[13px] disabled:opacity-40">다음</button>
        </div>
      )}
    </div>
  );
}
