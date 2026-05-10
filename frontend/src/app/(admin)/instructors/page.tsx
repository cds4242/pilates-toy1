"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { api } from "@/lib/api/client";
import { StatusBadge } from "@/components/design/StatusBadge";
import { toast } from "sonner";
import { usePageTitle } from "@/lib/hooks/use-page-title";

interface Instructor {
  id: number; name: string; phone: string | null; status: string; publicId: string; createdAt: string;
  specialty?: string | null; workingHoursMemo?: string | null;
}

interface InstructorForm {
  name: string;
  phone: string;
  specialty: string;
  workingHoursMemo: string;
}

const emptyForm: InstructorForm = { name: "", phone: "", specialty: "", workingHoursMemo: "" };

function formatPhone(phone: string | null): string {
  if (!phone || phone === "-") return "미등록";
  const digits = phone.replace(/\D/g, "");
  if (digits.length === 11) return `${digits.slice(0,3)}-${digits.slice(3,7)}-${digits.slice(7)}`;
  if (digits.length === 10) return `${digits.slice(0,3)}-${digits.slice(3,6)}-${digits.slice(6)}`;
  return phone;
}

export default function AdminInstructorsPage() {
  usePageTitle("강사 관리");
  const [instructors, setInstructors] = useState<Instructor[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editTarget, setEditTarget] = useState<Instructor | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const data = await api<Instructor[]>("get", "/api/admin/instructors");
      setInstructors(data);
    } catch { /* empty */ }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-bold text-text-title">강사 관리</h1>
        <button onClick={() => setShowAddModal(true)} className="rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-2.5 text-[13px] font-semibold text-text-title transition-colors">+ 강사 등록</button>
      </div>

      <div className="rounded-[18px] border border-border bg-white overflow-hidden">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-bg-section border-b border-border">
              {["프로필","이름","전화번호","전문 분야","상태","등록일"].map(h=>(
                <th key={h} className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan={6} className="text-center py-8 text-text-sub">로딩 중...</td></tr>
            : instructors.length === 0 ? <tr><td colSpan={6} className="text-center py-8 text-text-sub">등록된 강사가 없습니다</td></tr>
            : instructors.map(ins=>(
              <tr key={ins.id} className="border-b border-border last:border-0 hover:bg-bg-section cursor-pointer transition-colors" onDoubleClick={() => setEditTarget(ins)}>
                <td className="px-4 py-3.5"><div className="w-8 h-8 rounded-full bg-instructor-light flex items-center justify-center text-[13px] font-bold text-instructor">{ins.name.charAt(0)}</div></td>
                <td className="px-4 py-3.5 text-[15px] text-text-title font-semibold">{ins.name}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-body">{ins.phone && ins.phone !== "-" ? <a href={`tel:${ins.phone.replace(/\D/g,"")}`} className="text-pilates-dark hover:underline" onClick={(e) => e.stopPropagation()}>{formatPhone(ins.phone)}</a> : formatPhone(ins.phone)}</td>
                <td className="px-4 py-3.5 text-[15px] text-text-body">{ins.specialty || "-"}</td>
                <td className="px-4 py-3.5"><StatusBadge status={ins.status==="ACTIVE"?"active":"expired"} label={ins.status==="ACTIVE"?"활성":"비활성"}/></td>
                <td className="px-4 py-3.5 text-[13px] text-text-sub">{ins.createdAt?.slice(0,10)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {instructors.length <= 3 && !loading && (
        <div className="mt-6 rounded-[18px] border-2 border-dashed border-[var(--color-border)] p-6 text-center">
          <p className="text-[15px] text-text-sub">더 많은 강사를 등록하여 다양한 수업을 운영해보세요</p>
        </div>
      )}

      {showAddModal && (
        <InstructorModal
          mode="add"
          onClose={() => setShowAddModal(false)}
          onSuccess={() => { setShowAddModal(false); load(); }}
        />
      )}

      {editTarget && (
        <InstructorModal
          mode="edit"
          instructor={editTarget}
          onClose={() => setEditTarget(null)}
          onSuccess={() => { setEditTarget(null); load(); }}
        />
      )}
    </div>
  );
}

function InstructorModal({ mode, instructor, onClose, onSuccess }: {
  mode: "add" | "edit";
  instructor?: Instructor;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [form, setForm] = useState<InstructorForm>(
    instructor
      ? { name: instructor.name, phone: instructor.phone || "", specialty: instructor.specialty || "", workingHoursMemo: instructor.workingHoursMemo || "" }
      : { ...emptyForm }
  );
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const inputCls = "border border-[#DDDDDD] rounded-[8px] px-3.5 py-3 text-[15px] outline-none focus:border-pilates transition-colors w-full";

  const update = (key: keyof InstructorForm, value: string) => setForm(prev => ({ ...prev, [key]: value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim() || !form.phone.trim()) {
      toast.error("이름과 전화번호는 필수입니다");
      return;
    }
    setSaving(true);
    try {
      const body: Record<string, unknown> = { name: form.name.trim(), phone: form.phone.trim() };
      if (form.specialty.trim()) body.specialty = form.specialty.trim();
      if (form.workingHoursMemo.trim()) body.workingHoursMemo = form.workingHoursMemo.trim();

      if (mode === "add") {
        await api("post", "/api/admin/instructors", body);
        toast.success("강사가 등록되었습니다");
      } else {
        await api("patch", `/api/admin/instructors/${instructor!.id}`, body);
        toast.success("강사 정보가 수정되었습니다");
      }
      onSuccess();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : (mode === "add" ? "등록 실패" : "수정 실패"));
    } finally {
      setSaving(false);
    }
  };

  const handleDeactivate = async () => {
    if (!confirm("이 강사를 비활성화하시겠습니까?")) return;
    setDeleting(true);
    try {
      await api("delete", `/api/admin/instructors/${instructor!.id}`);
      toast.success("강사가 비활성화되었습니다");
      onSuccess();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "비활성화 실패");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-[18px] w-full max-w-[400px] p-6" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-[20px] font-bold text-text-title">{mode === "add" ? "강사 등록" : "강사 수정"}</h2>
          <button onClick={onClose}><X className="h-5 w-5 text-text-sub" /></button>
        </div>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-[13px] font-semibold text-text-title">이름 <span className="text-red-500">*</span></label>
            <input type="text" placeholder="강사 이름" value={form.name} onChange={(e) => update("name", e.target.value)} className={inputCls} required />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-[13px] font-semibold text-text-title">전화번호 <span className="text-red-500">*</span></label>
            <input type="tel" placeholder="01012345678" value={form.phone} onChange={(e) => update("phone", e.target.value)} className={inputCls} required />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-[13px] font-semibold text-text-title">전문 분야</label>
            <input type="text" placeholder="필라테스, 요가 (콤마 구분)" value={form.specialty} onChange={(e) => update("specialty", e.target.value)} className={inputCls} />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-[13px] font-semibold text-text-title">근무 시간 메모</label>
            <textarea placeholder="예: 월~금 09:00~18:00" value={form.workingHoursMemo} onChange={(e) => update("workingHoursMemo", e.target.value)} rows={2} className={inputCls + " resize-none"} />
          </div>
          <button type="submit" disabled={saving} className="bg-pilates hover:bg-pilates-dark text-text-title rounded-[8px] py-3.5 text-[15px] font-semibold transition-all disabled:opacity-60">
            {saving ? "저장 중..." : mode === "add" ? "등록하기" : "수정하기"}
          </button>
          {mode === "edit" && (
            <button type="button" onClick={handleDeactivate} disabled={deleting} className="border border-red-300 text-red-500 hover:bg-red-50 rounded-[8px] py-3 text-[15px] font-semibold transition-all disabled:opacity-60">
              {deleting ? "처리 중..." : "비활성화"}
            </button>
          )}
        </form>
      </div>
    </div>
  );
}
