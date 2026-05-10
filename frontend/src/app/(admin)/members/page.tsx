"use client";

import { useEffect, useRef, useState } from "react";
import { Search, X, Save, Trash2, User, CreditCard, FileText } from "lucide-react";
import { usePageTitle } from "@/lib/hooks/use-page-title";
import { api } from "@/lib/api/client";
import { adminApi } from "@/lib/api/admin";
import type { PageResponse } from "@/lib/types/api";
import { StatusBadge } from "@/components/design/StatusBadge";
import { HelpTip, PageGuide } from "@/components/design/HelpTip";
import {
  AdminPageHeader,
  AdminSearchBox,
  AdminPrimaryButton,
  AdminGhostButton,
  AdminPlusIcon,
} from "@/components/design/AdminPageHeader";
import { toast } from "sonner";

interface AdminMember {
  id: number; name: string; phone: string; gender: string; status: string; activeMembership: string | null; remainingInfo: string; expiryDate: string | null; attendanceRate: string; createdAt: string;
}

interface MembershipInfo {
  id: number; passName: string; status: string; totalCount: number; remainingCount: number; unlimited: boolean; startDate: string | null; endDate: string | null;
}

interface MemoInfo {
  id: number; content: string; writerName: string; createdAt: string; updatedAt: string;
}

interface MemberDetail {
  id: number; name: string; phone: string; birthDate: string | null; gender: string; status: string; profileImageUrl: string | null; createdAt: string;
  memberships: MembershipInfo[];
  attendanceRate: { overallRate: number; recent90DayRate: number } | null;
  noShowCount: number;
  memos: MemoInfo[];
}

function formatPhone(phone: string | null): string {
  if (!phone) return "-";
  const d = phone.replace(/\D/g, "");
  if (d.length === 11) return `${d.slice(0,3)}-${d.slice(3,7)}-${d.slice(7)}`;
  if (d.length === 10) return `${d.slice(0,3)}-${d.slice(3,6)}-${d.slice(6)}`;
  return phone;
}

function formatExpiry(dateStr: string | null) {
  if (!dateStr) return { text: "-", color: "" };
  const diff = Math.ceil((new Date(dateStr).getTime() - Date.now()) / 86400000);
  const short = dateStr.slice(5).replace("-","/");
  if (diff < 0) return { text: "만료됨", color: "text-[var(--color-error)] font-semibold" };
  if (diff <= 7) return { text: `${short} (D-${diff})`, color: "text-[var(--color-error)] font-semibold" };
  if (diff <= 30) return { text: `${short} (D-${diff})`, color: "text-[var(--color-warning)]" };
  return { text: short, color: "text-text-body" };
}

function AttendanceBar({ rate }: { rate: string }) {
  if (!rate || rate === "-") return <span className="text-text-sub">-</span>;
  const pct = parseInt(rate);
  const color = pct >= 80 ? "bg-green-500" : pct >= 50 ? "bg-yellow-500" : "bg-red-500";
  return (
    <div>
      <span className="text-[14px] text-text-body">{rate}</span>
      <div className="w-[60px] h-[3px] bg-gray-200 rounded-full mt-1">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export default function AdminMembersPage() {
  usePageTitle("회원 관리");
  const [members, setMembers] = useState<AdminMember[]>([]);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [detailMemberId, setDetailMemberId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState("");
  const [membershipFilter, setMembershipFilter] = useState("");
  const [sortKey, setSortKey] = useState<string>("");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("asc");
  const fileRef = useRef<HTMLInputElement>(null);

  const searchRef = useRef(search);
  searchRef.current = search;

  const load = async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { search: searchRef.current, page, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const res = await api<PageResponse<AdminMember>>("get", "/api/admin/members", params);
      setMembers(res.content); setTotal(res.totalElements);
    } catch { /* empty */ }
    finally { setLoading(false); }
  };

  // debounce search: re-fetch 300ms after typing stops
  useEffect(() => {
    const timer = setTimeout(() => { setPage(0); load(); }, 300);
    return () => clearTimeout(timer);
  }, [search]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load(); }, [page, statusFilter]); // eslint-disable-line react-hooks/exhaustive-deps
  const handleSearch = (e: React.FormEvent) => { e.preventDefault(); setPage(0); load(); };

  const handleSort = (key: string) => {
    if (sortKey === key) setSortDir(sortDir === "asc" ? "desc" : "asc");
    else { setSortKey(key); setSortDir("asc"); }
  };

  const filteredByMembership = membershipFilter
    ? membershipFilter === "none"
      ? members.filter(m => !m.activeMembership || m.activeMembership === "-")
      : members.filter(m => m.activeMembership === membershipFilter)
    : members;

  const sortedMembers = [...filteredByMembership].sort((a, b) => {
    if (!sortKey) return 0;
    let va: string | number = (a as any)[sortKey] || "";
    let vb: string | number = (b as any)[sortKey] || "";
    if (sortKey === "attendanceRate") { va = parseInt(va as string) || 0; vb = parseInt(vb as string) || 0; }
    if (sortKey === "expiryDate") { va = (va as string) || "9999-99-99"; vb = (vb as string) || "9999-99-99"; }
    if (typeof va === "number") return sortDir === "asc" ? va - (vb as number) : (vb as number) - va;
    return sortDir === "asc" ? String(va).localeCompare(String(vb)) : String(vb).localeCompare(String(va));
  });

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

  const handleExportCsv = () => {
    const header = "이름,휴대폰,정기권,잔여,만료일,출석률,상태\n";
    const rows = members
      .map(
        (m) =>
          `${m.name},${formatPhone(m.phone)},${m.activeMembership || "-"},${m.remainingInfo || "-"},${m.expiryDate || "-"},${m.attendanceRate || "-"},${m.status}`,
      )
      .join("\n");
    const blob = new Blob(["﻿" + header + rows], {
      type: "text/csv;charset=utf-8;",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `회원목록_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div>
      <AdminPageHeader
        eyebrow="MEMBERS"
        title="회원 관리"
        sub={loading ? "회원 정보를 불러오는 중이에요." : `총 ${total}명의 회원이 등록되어 있어요.`}
        actions={
          <>
            <AdminSearchBox
              value={search}
              onChange={setSearch}
              placeholder="이름 · 휴대폰 검색..."
            />
            <AdminPrimaryButton
              onClick={() => setShowAddModal(true)}
              icon={<AdminPlusIcon />}
            >
              회원 등록
            </AdminPrimaryButton>
          </>
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <AdminGhostButton onClick={() => fileRef.current?.click()}>
          엑셀 일괄 등록
        </AdminGhostButton>
        <input
          ref={fileRef}
          type="file"
          accept=".xlsx,.xls"
          className="hidden"
          onChange={handleExcelUpload}
        />
        <AdminGhostButton onClick={handleExportCsv}>내보내기</AdminGhostButton>
        <HelpTip text="이름과 전화번호만으로 회원을 등록할 수 있습니다. 대량 등록은 엑셀 파일을 이용하세요." />
      </div>

      {/* removed-legacy-block-START
            const header = "이름,휴대폰,정기권,잔여,만료일,출석률,상태\n";
            const rows = members.map(m => `${m.name},${formatPhone(m.phone)},${m.activeMembership||"-"},${m.remainingInfo||"-"},${m.expiryDate||"-"},${m.attendanceRate||"-"},${m.status}`).join("\n");
            const blob = new Blob(["\uFEFF" + header + rows], { type: "text/csv;charset=utf-8;" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url; a.download = `회원목록_${new Date().toISOString().slice(0,10)}.csv`; a.click();
            URL.revokeObjectURL(url);
          }} className="..." > end-removed */}

      <PageGuide text="회원을 클릭하면 상세 정보를 확인하고 수강권을 발급할 수 있습니다. 상단 필터와 정렬을 활용하여 원하는 회원을 빠르게 찾아보세요." />

      <form onSubmit={handleSearch} className="mb-4 flex flex-wrap gap-2">
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPage(0);
          }}
          className="min-w-[120px] rounded-full border border-[#F0EBE8] bg-white px-4 py-2 text-[13px] text-[#2A2A2C] outline-none transition-colors focus:border-[#FAD4DE]"
        >
          <option value="">상태 전체</option>
          <option value="ACTIVE">활성</option>
          <option value="WITHDRAWN">탈퇴</option>
        </select>
        <select
          value={membershipFilter}
          onChange={(e) => {
            setMembershipFilter(e.target.value);
            setPage(0);
          }}
          className="min-w-[140px] rounded-full border border-[#F0EBE8] bg-white px-4 py-2 text-[13px] text-[#2A2A2C] outline-none transition-colors focus:border-[#FAD4DE]"
        >
          <option value="">정기권 전체</option>
          <option value="8회권">8회권</option>
          <option value="12회권">12회권</option>
          <option value="무제한권">무제한권</option>
          <option value="개인 10회권">개인 10회권</option>
          <option value="none">정기권 없음</option>
        </select>
      </form>

      <div className="flex gap-2 mb-4 flex-wrap">
        {[
          { label: "만료 임박", action: () => { setSortKey("expiryDate"); setSortDir("asc"); setMembershipFilter(""); setStatusFilter("ACTIVE"); } },
          { label: "정기권 없음", action: () => { setMembershipFilter("none"); setStatusFilter(""); } },
          { label: "출석률 낮은순", action: () => { setSortKey("attendanceRate"); setSortDir("asc"); setMembershipFilter(""); } },
          { label: "전체 보기", action: () => { setSortKey(""); setMembershipFilter(""); setStatusFilter(""); setSearch(""); } },
        ].map(p => (
          <button key={p.label} onClick={p.action}
            className="rounded-full border border-border px-3 py-1.5 text-[12px] text-text-body hover:border-pilates hover:text-pilates-dark transition-colors">
            {p.label}
          </button>
        ))}
      </div>

      {/* PC 테이블 */}
      <div className="hidden md:block rounded-[18px] border border-border bg-white overflow-hidden">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-bg-section border-b border-border">
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">프로필</th>
              <th onClick={() => handleSort("name")} className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub cursor-pointer hover:text-text-title select-none">
                이름 {sortKey === "name" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">휴대폰</th>
              <th onClick={() => handleSort("activeMembership")} className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub cursor-pointer hover:text-text-title select-none">
                정기권 {sortKey === "activeMembership" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">잔여</th>
              <th onClick={() => handleSort("expiryDate")} className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub cursor-pointer hover:text-text-title select-none">
                만료일 {sortKey === "expiryDate" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th onClick={() => handleSort("attendanceRate")} className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub cursor-pointer hover:text-text-title select-none">
                출석률 {sortKey === "attendanceRate" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">상태</th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub"></th>
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan={9} className="text-center py-8 text-text-sub">로딩 중...</td></tr>
            : sortedMembers.length === 0 ? <tr><td colSpan={9} className="text-center py-8 text-text-sub">회원이 없습니다</td></tr>
            : sortedMembers.map(m=>{
              const exp = formatExpiry(m.expiryDate);
              const avatarCls = m.gender === "MALE" ? "bg-blue-100 text-blue-700" : "bg-pilates-light text-pilates-dark";
              const rowBg = !m.activeMembership || m.activeMembership === "-" ? "bg-amber-50" : "";
              return (
              <tr key={m.id} className={`border-b border-border last:border-0 hover:bg-bg-section cursor-pointer transition-colors ${rowBg}`} onClick={() => setDetailMemberId(m.id)}>
                <td className="px-4 py-3.5"><div className={`w-8 h-8 rounded-full flex items-center justify-center text-[13px] font-bold ${avatarCls}`}>{m.name.charAt(0)}</div></td>
                <td className="px-4 py-3.5 text-[15px] text-text-title">{m.name}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-title">{formatPhone(m.phone)}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-body">{m.activeMembership || <span className="text-text-sub">미등록</span>}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-body">{m.remainingInfo||"-"}</td>
                <td className={`px-4 py-3.5 text-[15px] ${exp.color}`}>{exp.text}</td>
                <td className="px-4 py-3.5"><AttendanceBar rate={m.attendanceRate||"-"} /></td>
                <td className="px-4 py-3.5"><StatusBadge status={m.status==="ACTIVE"?"active":"expired"} label={m.status==="ACTIVE"?"활성":m.status==="WITHDRAWN"?"탈퇴":"만료"}/></td>
                <td className="px-4 py-3.5 text-text-sub">›</td>
              </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* 모바일 카드 */}
      <div className="md:hidden flex flex-col gap-3">
        {loading ? <div className="text-center py-8 text-text-sub">로딩 중...</div>
        : sortedMembers.length === 0 ? <div className="text-center py-8 text-text-sub">회원이 없습니다</div>
        : sortedMembers.map(m=>{
          const mobileAvatarCls = m.gender === "MALE" ? "bg-blue-100 text-blue-700" : "bg-pilates-light text-pilates-dark";
          return (
          <div key={m.id} className={`rounded-[18px] border border-border ${!m.activeMembership || m.activeMembership === "-" ? "bg-amber-50" : "bg-white"} p-4 flex items-center justify-between cursor-pointer`} onClick={() => setDetailMemberId(m.id)}>
            <div className="flex items-center gap-3 flex-1">
              <div className={`w-9 h-9 rounded-full flex items-center justify-center text-[14px] font-bold shrink-0 ${mobileAvatarCls}`}>{m.name.charAt(0)}</div>
              <div className="flex-1 min-w-0">
                <p className="text-[15px] font-semibold text-text-title">{m.name}</p>
                <p className="text-[13px] text-text-sub mt-0.5">{m.activeMembership||"정기권 없음"} | {formatPhone(m.phone)}</p>
              </div>
            </div>
            <StatusBadge status={m.status==="ACTIVE"?"active":"expired"} label={m.status==="ACTIVE"?"활성":"만료"}/>
          </div>
          );
        })}
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

      {/* 회원 상세 모달 */}
      {detailMemberId !== null && (
        <MemberDetailModal
          memberId={detailMemberId}
          onClose={() => setDetailMemberId(null)}
          onUpdate={() => { setDetailMemberId(null); load(); }}
        />
      )}
    </div>
  );
}

interface MembershipPassItem {
  id: number; name: string; price: number; totalCount: number | null; validityDays: number; unlimited: boolean; lessonTypes: { id: number; name: string }[];
}

function MemberDetailModal({ memberId, onClose, onUpdate }: { memberId: number; onClose: () => void; onUpdate: () => void }) {
  const [detail, setDetail] = useState<MemberDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editName, setEditName] = useState("");
  const [editPhone, setEditPhone] = useState("");
  const [isEditing, setIsEditing] = useState(false);
  const [memoText, setMemoText] = useState("");
  const [memoSaving, setMemoSaving] = useState(false);
  const [showWithdrawConfirm, setShowWithdrawConfirm] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);
  const [activeTab, setActiveTab] = useState<"info" | "membership" | "memo">("info");
  const [issueModalOpen, setIssueModalOpen] = useState(false);
  const [passes, setPasses] = useState<MembershipPassItem[]>([]);
  const [passesLoading, setPassesLoading] = useState(false);
  const [selectedPassId, setSelectedPassId] = useState<number | null>(null);
  const [issuing, setIssuing] = useState(false);

  const inputCls = "border border-[#DDDDDD] rounded-[8px] px-3.5 py-2.5 text-[15px] outline-none focus:border-pilates transition-colors w-full";

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const res = await api<MemberDetail>("get", `/api/admin/members/${memberId}`);
        setDetail(res);
        setEditName(res.name);
        setEditPhone(res.phone);
      } catch {
        toast.error("회원 정보를 불러올 수 없습니다");
        onClose();
      } finally {
        setLoading(false);
      }
    })();
  }, [memberId]);

  const openIssueModal = async () => {
    setIssueModalOpen(true);
    setSelectedPassId(null);
    setPassesLoading(true);
    try {
      const res = await api<MembershipPassItem[]>("get", "/api/admin/membership-passes");
      setPasses(res);
    } catch {
      toast.error("정기권 종류를 불러올 수 없습니다");
    } finally {
      setPassesLoading(false);
    }
  };

  const handleIssueMembership = async () => {
    if (!selectedPassId) return;
    const pass = passes.find(p => p.id === selectedPassId);
    if (!pass) return;
    setIssuing(true);
    try {
      await api("post", "/api/admin/memberships", {
        memberId,
        membershipPassId: pass.id,
        totalCount: pass.totalCount ?? 0,
        price: pass.price,
        validityDays: pass.validityDays,
        unlimited: pass.unlimited,
        lessonTypeIds: pass.lessonTypes.map(lt => lt.id),
      });
      toast.success("정기권이 발급되었습니다");
      setIssueModalOpen(false);
      // 회원 상세 새로고침
      const res = await api<MemberDetail>("get", `/api/admin/members/${memberId}`);
      setDetail(res);
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "정기권 발급에 실패했습니다");
    } finally {
      setIssuing(false);
    }
  };

  const handleSave = async () => {
    if (!detail) return;
    setSaving(true);
    try {
      await api("patch", `/api/admin/members/${memberId}`, { name: editName, phone: editPhone });
      toast.success("회원 정보가 수정되었습니다");
      setIsEditing(false);
      onUpdate();
    } catch {
      toast.error("회원 정보 수정에 실패했습니다.");
      setSaving(false);
    }
  };

  const handleMemoSubmit = async () => {
    if (!memoText.trim()) return;
    setMemoSaving(true);
    try {
      const newMemo = await api<MemoInfo>("post", `/api/admin/members/${memberId}/memos`, { content: memoText });
      setDetail(prev => prev ? { ...prev, memos: [...prev.memos, newMemo] } : prev);
      setMemoText("");
      toast.success("메모가 저장되었습니다");
    } catch {
      toast.error("메모 저장에 실패했습니다");
    } finally {
      setMemoSaving(false);
    }
  };

  const handleWithdraw = async () => {
    setWithdrawing(true);
    try {
      await api("delete", `/api/admin/members/${memberId}`);
      toast.success("회원이 탈퇴 처리되었습니다");
      onUpdate();
    } catch {
      toast.error("탈퇴 처리에 실패했습니다");
    } finally {
      setWithdrawing(false);
      setShowWithdrawConfirm(false);
    }
  };

  const formatDate = (d: string | null) => {
    if (!d) return "-";
    return new Date(d).toLocaleDateString("ko-KR");
  };

  const genderLabel = (g: string) => {
    if (g === "MALE") return "남성";
    if (g === "FEMALE") return "여성";
    return g || "-";
  };

  const statusLabel = (s: string) => {
    if (s === "ACTIVE") return "활성";
    if (s === "WITHDRAWN") return "탈퇴";
    return "만료";
  };

  const tabs = [
    { key: "info" as const, label: "기본 정보", icon: User },
    { key: "membership" as const, label: "정기권", icon: CreditCard },
    { key: "memo" as const, label: "메모", icon: FileText },
  ];

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-[18px] w-full max-w-[540px] max-h-[85vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
        {/* 헤더 */}
        <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-border shrink-0">
          <h2 className="text-[20px] font-bold text-text-title">회원 상세</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-text-sub" /></button>
        </div>

        {loading || !detail ? (
          <div className="flex-1 flex items-center justify-center py-12 text-text-sub text-[15px]">로딩 중...</div>
        ) : (
          <>
            {/* 프로필 요약 */}
            <div className="px-6 py-4 flex items-center gap-4 border-b border-border shrink-0">
              <div className="w-12 h-12 rounded-full bg-pilates-light flex items-center justify-center text-[18px] font-bold text-pilates-dark shrink-0">
                {detail.name.charAt(0)}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-[15px] font-semibold text-text-title">{detail.name}</p>
                  <StatusBadge status={detail.status === "ACTIVE" ? "active" : "expired"} label={statusLabel(detail.status)} />
                </div>
                <p className="text-[13px] text-text-sub mt-0.5">{detail.phone} | {genderLabel(detail.gender)} | 가입 {formatDate(detail.createdAt)}</p>
              </div>
            </div>

            {/* 탭 */}
            <div className="flex border-b border-border px-6 shrink-0">
              {tabs.map(tab => (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  className={`flex items-center gap-1.5 px-4 py-3 text-[13px] font-semibold border-b-2 transition-colors ${
                    activeTab === tab.key ? "border-pilates text-pilates-dark" : "border-transparent text-text-sub hover:text-text-body"
                  }`}
                >
                  <tab.icon className="h-3.5 w-3.5" />
                  {tab.label}
                </button>
              ))}
            </div>

            {/* 탭 컨텐츠 */}
            <div className="flex-1 overflow-y-auto px-6 py-4">
              {activeTab === "info" && (
                <div className="flex flex-col gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">이름</label>
                    {isEditing ? (
                      <input type="text" value={editName} onChange={(e) => setEditName(e.target.value)} className={inputCls} />
                    ) : (
                      <p className="text-[15px] text-text-body px-3.5 py-2.5">{detail.name}</p>
                    )}
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">휴대폰 번호</label>
                    {isEditing ? (
                      <input type="tel" value={editPhone} onChange={(e) => setEditPhone(e.target.value)} className={inputCls} />
                    ) : (
                      <p className="text-[15px] text-text-body px-3.5 py-2.5">{detail.phone}</p>
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="flex flex-col gap-1.5">
                      <label className="text-[13px] font-semibold text-text-title">성별</label>
                      <p className="text-[15px] text-text-body px-3.5 py-2.5">{genderLabel(detail.gender)}</p>
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label className="text-[13px] font-semibold text-text-title">가입일</label>
                      <p className="text-[15px] text-text-body px-3.5 py-2.5">{formatDate(detail.createdAt)}</p>
                    </div>
                  </div>
                  {detail.attendanceRate && (
                    <div className="grid grid-cols-2 gap-4">
                      <div className="flex flex-col gap-1.5">
                        <label className="text-[13px] font-semibold text-text-title">전체 출석률</label>
                        <p className="text-[15px] text-text-body px-3.5 py-2.5">{detail.attendanceRate.overallRate}%</p>
                      </div>
                      <div className="flex flex-col gap-1.5">
                        <label className="text-[13px] font-semibold text-text-title">최근 90일 출석률</label>
                        <p className="text-[15px] text-text-body px-3.5 py-2.5">{detail.attendanceRate.recent90DayRate}%</p>
                      </div>
                    </div>
                  )}
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">노쇼 횟수</label>
                    <p className="text-[15px] text-text-body px-3.5 py-2.5">{detail.noShowCount}회</p>
                  </div>

                  {/* 수정 버튼 */}
                  <div className="flex gap-2 mt-2">
                    {isEditing ? (
                      <>
                        <button onClick={handleSave} disabled={saving} className="flex items-center gap-1.5 rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-2.5 text-[13px] font-semibold text-text-title transition-colors disabled:opacity-60">
                          <Save className="h-3.5 w-3.5" />
                          {saving ? "저장 중..." : "저장"}
                        </button>
                        <button onClick={() => { setIsEditing(false); setEditName(detail.name); setEditPhone(detail.phone); }} className="rounded-[8px] border border-border px-4 py-2.5 text-[13px] font-semibold text-text-body transition-colors hover:border-pilates">
                          취소
                        </button>
                      </>
                    ) : (
                      <button onClick={() => setIsEditing(true)} className="rounded-[8px] border border-border px-4 py-2.5 text-[13px] font-semibold text-text-body transition-colors hover:border-pilates">
                        정보 수정
                      </button>
                    )}
                  </div>
                </div>
              )}

              {activeTab === "membership" && (
                <div className="flex flex-col gap-3">
                  <button
                    onClick={openIssueModal}
                    className="self-end rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-2.5 text-[13px] font-semibold text-text-title transition-colors"
                  >
                    + 정기권 발급
                  </button>
                  {detail.memberships.length === 0 ? (
                    <p className="text-[15px] text-text-sub text-center py-8">등록된 정기권이 없습니다</p>
                  ) : detail.memberships.map(ms => (
                    <div key={ms.id} className="rounded-[12px] border border-border p-4">
                      <div className="flex items-center justify-between mb-2">
                        <p className="text-[15px] font-semibold text-text-title">{ms.passName}</p>
                        <StatusBadge
                          status={ms.status === "ACTIVE" ? "active" : ms.status === "EXPIRING_SOON" ? "expiring" : "expired"}
                          label={ms.status === "ACTIVE" ? "활성" : ms.status === "EXPIRING_SOON" ? "만료임박" : "만료"}
                        />
                      </div>
                      <div className="grid grid-cols-2 gap-2 text-[13px]">
                        <div>
                          <span className="text-text-sub">잔여: </span>
                          <span className="text-text-body font-semibold">{ms.unlimited ? "무제한" : `${ms.remainingCount} / ${ms.totalCount}회`}</span>
                        </div>
                        <div>
                          <span className="text-text-sub">기간: </span>
                          <span className="text-text-body">{formatDate(ms.startDate)} ~ {formatDate(ms.endDate)}</span>
                        </div>
                      </div>
                    </div>
                  ))}

                  {/* 정기권 발급 모달 */}
                  {issueModalOpen && (
                    <div className="fixed inset-0 z-[60] bg-black/40 flex items-center justify-center p-4" onClick={() => setIssueModalOpen(false)}>
                      <div className="bg-white rounded-[18px] w-full max-w-[480px] max-h-[80vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-border shrink-0">
                          <h3 className="text-[18px] font-bold text-text-title">정기권 발급</h3>
                          <button onClick={() => setIssueModalOpen(false)}><X className="h-5 w-5 text-text-sub" /></button>
                        </div>
                        <div className="flex-1 overflow-y-auto px-6 py-4">
                          {passesLoading ? (
                            <p className="text-[15px] text-text-sub text-center py-8">로딩 중...</p>
                          ) : passes.length === 0 ? (
                            <p className="text-[15px] text-text-sub text-center py-8">등록된 정기권 종류가 없습니다</p>
                          ) : (
                            <div className="flex flex-col gap-3">
                              {passes.map(pass => (
                                <div
                                  key={pass.id}
                                  onClick={() => setSelectedPassId(pass.id)}
                                  className={`rounded-[18px] border p-4 cursor-pointer transition-colors ${
                                    selectedPassId === pass.id
                                      ? "border-pilates bg-pilates-light/30"
                                      : "border-border hover:border-pilates/50"
                                  }`}
                                >
                                  <p className="text-[15px] font-semibold text-text-title mb-2">{pass.name}</p>
                                  <div className="grid grid-cols-3 gap-2 text-[13px]">
                                    <div>
                                      <span className="text-text-sub">가격 </span>
                                      <span className="text-text-body font-semibold">{Number(pass.price).toLocaleString()}원</span>
                                    </div>
                                    <div>
                                      <span className="text-text-sub">횟수 </span>
                                      <span className="text-text-body font-semibold">{pass.unlimited ? "무제한" : `${pass.totalCount}회`}</span>
                                    </div>
                                    <div>
                                      <span className="text-text-sub">기간 </span>
                                      <span className="text-text-body font-semibold">{pass.validityDays}일</span>
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                        <div className="px-6 py-4 border-t border-border shrink-0">
                          <button
                            onClick={handleIssueMembership}
                            disabled={!selectedPassId || issuing}
                            className="w-full rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-3 text-[15px] font-semibold text-text-title transition-colors disabled:opacity-40"
                          >
                            {issuing ? "발급 중..." : "발급하기"}
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {activeTab === "memo" && (
                <div className="flex flex-col gap-4">
                  {/* 메모 입력 */}
                  <div className="flex flex-col gap-2">
                    <textarea
                      value={memoText}
                      onChange={(e) => setMemoText(e.target.value)}
                      placeholder="메모를 입력하세요..."
                      rows={3}
                      className="border border-[#DDDDDD] rounded-[8px] px-3.5 py-2.5 text-[15px] outline-none focus:border-pilates transition-colors w-full resize-none"
                    />
                    <button
                      onClick={handleMemoSubmit}
                      disabled={!memoText.trim() || memoSaving}
                      className="self-end rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-2 text-[13px] font-semibold text-text-title transition-colors disabled:opacity-60"
                    >
                      {memoSaving ? "저장 중..." : "메모 저장"}
                    </button>
                  </div>

                  {/* 메모 목록 */}
                  {detail.memos.length === 0 ? (
                    <p className="text-[15px] text-text-sub text-center py-4">작성된 메모가 없습니다</p>
                  ) : (
                    <div className="flex flex-col gap-2">
                      {detail.memos.map(memo => (
                        <div key={memo.id} className="rounded-[12px] border border-border p-3">
                          <p className="text-[15px] text-text-body whitespace-pre-wrap">{memo.content}</p>
                          <p className="text-[11px] text-text-sub mt-2">{memo.writerName} | {formatDate(memo.createdAt)}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* 하단 탈퇴 버튼 */}
            <div className="px-6 py-4 border-t border-border shrink-0">
              {showWithdrawConfirm ? (
                <div className="flex items-center justify-between">
                  <p className="text-[13px] text-[#E76F51] font-semibold">정말 이 회원을 탈퇴 처리하시겠습니까?</p>
                  <div className="flex gap-2">
                    <button onClick={handleWithdraw} disabled={withdrawing} className="rounded-[8px] bg-[#E76F51] hover:bg-[#d45a3d] px-4 py-2 text-[13px] font-semibold text-white transition-colors disabled:opacity-60">
                      {withdrawing ? "처리 중..." : "확인"}
                    </button>
                    <button onClick={() => setShowWithdrawConfirm(false)} className="rounded-[8px] border border-border px-4 py-2 text-[13px] font-semibold text-text-body transition-colors">
                      취소
                    </button>
                  </div>
                </div>
              ) : (
                <button onClick={() => setShowWithdrawConfirm(true)} className="flex items-center gap-1.5 rounded-[8px] border border-[#E76F51] px-4 py-2 text-[13px] font-semibold text-[#E76F51] transition-colors hover:bg-[#FDECEA]">
                  <Trash2 className="h-3.5 w-3.5" />
                  회원 탈퇴
                </button>
              )}
            </div>
          </>
        )}
      </div>
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
    if (!name.trim() || !phone.trim()) {
      toast.error("이름과 전화번호를 입력해주세요");
      return;
    }
    setLoading(true);
    try {
      await api("post", "/api/admin/members", { name: name.trim(), phone: phone.replace(/-/g, "") });
      toast.success("회원이 등록되었습니다");
      onSuccess();
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
