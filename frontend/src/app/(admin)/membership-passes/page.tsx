"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { usePageTitle } from "@/lib/hooks/use-page-title";
import { api } from "@/lib/api/client";
import { StatusBadge } from "@/components/design/StatusBadge";
import { PageGuide } from "@/components/design/HelpTip";
import {
  AdminPageHeader,
  AdminPrimaryButton,
  AdminPlusIcon,
} from "@/components/design/AdminPageHeader";
import { toast } from "sonner";

interface LessonType {
  id: number;
  name: string;
}

interface MembershipPass {
  id: number;
  publicId: string;
  name: string;
  price: number;
  totalCount: number | null;
  validityDays: number;
  unlimited: boolean;
  monthlyLimit: number | null;
  displayOrder: number;
  visible: boolean;
  active: boolean;
  saleStartDate: string | null;
  saleEndDate: string | null;
  category: string | null;
  description: string | null;
  lessonTypes: LessonType[];
  createdAt: string;
}

interface FormData {
  name: string;
  category: string;
  price: string;
  totalCount: string;
  validityDays: string;
  unlimited: boolean;
  monthlyLimit: string;
  displayOrder: string;
  visible: boolean;
  saleStartDate: string;
  saleEndDate: string;
  description: string;
  lessonTypeIds: number[];
}

const EMPTY_FORM: FormData = {
  name: "",
  category: "PERSONAL",
  price: "",
  totalCount: "",
  validityDays: "",
  unlimited: false,
  monthlyLimit: "",
  displayOrder: "0",
  visible: true,
  saleStartDate: "",
  saleEndDate: "",
  description: "",
  lessonTypeIds: [],
};

const CATEGORY_LABELS: Record<string, string> = {
  PERSONAL: "개인",
  GROUP: "그룹",
  UNLIMITED: "무제한",
};

function formatSalePeriod(start: string | null, end: string | null): string {
  const s = start || "즉시";
  const e = end || "무기한";
  return `${s} ~ ${e}`;
}

export default function MembershipPassesPage() {
  usePageTitle("수강권 관리");
  const [passes, setPasses] = useState<MembershipPass[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editTarget, setEditTarget] = useState<MembershipPass | null>(null);
  const [form, setForm] = useState<FormData>(EMPTY_FORM);
  const [lessonTypes, setLessonTypes] = useState<LessonType[]>([]);
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const res = await api<MembershipPass[]>("get", "/api/admin/membership-passes");
      setPasses(res);
    } catch {
      toast.error("수강권 목록을 불러올 수 없습니다");
    } finally {
      setLoading(false);
    }
  };

  const loadLessonTypes = async () => {
    try {
      const res = await api<LessonType[]>("get", "/api/lesson-types");
      setLessonTypes(res);
    } catch { /* ignore */ }
  };

  useEffect(() => { load(); }, []);

  const openCreateModal = () => {
    setForm(EMPTY_FORM);
    setEditTarget(null);
    setModalMode("create");
    loadLessonTypes();
  };

  const openEditModal = (pass: MembershipPass) => {
    setEditTarget(pass);
    setForm({
      name: pass.name,
      category: pass.category || "PERSONAL",
      price: String(pass.price),
      totalCount: pass.totalCount != null ? String(pass.totalCount) : "",
      validityDays: String(pass.validityDays),
      unlimited: pass.unlimited,
      monthlyLimit: pass.monthlyLimit != null ? String(pass.monthlyLimit) : "",
      displayOrder: String(pass.displayOrder),
      visible: pass.visible,
      saleStartDate: pass.saleStartDate || "",
      saleEndDate: pass.saleEndDate || "",
      description: pass.description || "",
      lessonTypeIds: pass.lessonTypes.map((lt) => lt.id),
    });
    setModalMode("edit");
    loadLessonTypes();
  };

  const closeModal = () => {
    setModalMode(null);
    setEditTarget(null);
  };

  const handleSubmit = async () => {
    if (!form.name.trim()) { toast.error("이름을 입력해주세요"); return; }
    if (!form.price) { toast.error("가격을 입력해주세요"); return; }
    if (!form.validityDays) { toast.error("유효기간을 입력해주세요"); return; }
    if (form.lessonTypeIds.length === 0) { toast.error("수업 유형을 1개 이상 선택해주세요"); return; }

    setSaving(true);
    try {
      const body: Record<string, unknown> = {
        name: form.name.trim(),
        category: form.category,
        price: Number(form.price),
        totalCount: form.unlimited ? null : (form.totalCount ? Number(form.totalCount) : null),
        validityDays: Number(form.validityDays),
        unlimited: form.unlimited,
        monthlyLimit: form.unlimited && form.monthlyLimit ? Number(form.monthlyLimit) : null,
        displayOrder: Number(form.displayOrder) || 0,
        visible: form.visible,
        saleStartDate: form.saleStartDate || null,
        saleEndDate: form.saleEndDate || null,
        description: form.description.trim() || null,
        lessonTypeIds: form.lessonTypeIds,
      };

      if (modalMode === "create") {
        await api("post", "/api/admin/membership-passes", body);
        toast.success("수강권이 등록되었습니다");
      } else if (editTarget) {
        await api("patch", `/api/admin/membership-passes/${editTarget.id}`, body);
        toast.success("수강권이 수정되었습니다");
      }
      closeModal();
      load();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "저장에 실패했습니다");
    } finally {
      setSaving(false);
    }
  };

  const handleToggleActive = async () => {
    if (!editTarget) return;
    setSaving(true);
    try {
      if (editTarget.active) {
        await api("delete", `/api/admin/membership-passes/${editTarget.id}`);
        toast.success("판매가 중지되었습니다");
      } else {
        await api("patch", `/api/admin/membership-passes/${editTarget.id}`, { active: true });
        toast.success("판매가 재개되었습니다");
      }
      closeModal();
      load();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "처리에 실패했습니다");
    } finally {
      setSaving(false);
    }
  };

  const toggleLessonType = (id: number) => {
    setForm((prev) => ({
      ...prev,
      lessonTypeIds: prev.lessonTypeIds.includes(id)
        ? prev.lessonTypeIds.filter((x) => x !== id)
        : [...prev.lessonTypeIds, id],
    }));
  };

  const inputCls =
    "border border-[#DDDDDD] rounded-[8px] px-3.5 py-2.5 text-[15px] outline-none focus:border-pilates transition-colors w-full";

  return (
    <div>
      <AdminPageHeader
        eyebrow="MEMBERSHIP PASSES"
        title="정기권 상품 관리"
        sub={
          loading
            ? "수강권 상품 정보를 불러오는 중이에요."
            : `총 ${passes.length}개의 상품이 등록되어 있어요.`
        }
        actions={
          <AdminPrimaryButton onClick={openCreateModal} icon={<AdminPlusIcon />}>
            상품 등록
          </AdminPrimaryButton>
        }
      />

      <PageGuide text="수강권 상품을 등록하고 관리할 수 있습니다. 비활성화하면 신규 판매가 중지되지만, 이미 발급된 회원은 계속 사용할 수 있습니다." />

      {/* PC 테이블 */}
      <div className="hidden md:block rounded-[18px] border border-border bg-white overflow-hidden">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-bg-section border-b border-border">
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">이름</th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">카테고리</th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">가격</th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">횟수</th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">유효기간</th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">노출</th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">판매상태</th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">판매기간</th>
              <th className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">순서</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={9} className="text-center py-8 text-text-sub">로딩 중...</td></tr>
            ) : passes.length === 0 ? (
              <tr><td colSpan={9} className="text-center py-8 text-text-sub">등록된 수강권이 없습니다</td></tr>
            ) : (
              passes.map((p) => (
                <tr
                  key={p.id}
                  className="border-b border-border last:border-0 hover:bg-bg-section cursor-pointer transition-colors"
                  onClick={() => openEditModal(p)}
                >
                  <td className="px-4 py-3.5 text-[15px] font-semibold text-text-title">{p.name}</td>
                  <td className="px-4 py-3.5 text-[15px] text-text-body">{CATEGORY_LABELS[p.category || ""] || p.category || "-"}</td>
                  <td className="px-4 py-3.5 text-[15px] text-text-body">{Number(p.price).toLocaleString()}원</td>
                  <td className="px-4 py-3.5 text-[15px] text-text-body">{p.unlimited ? "무제한" : `${p.totalCount}회`}</td>
                  <td className="px-4 py-3.5 text-[15px] text-text-body">{p.validityDays}일</td>
                  <td className="px-4 py-3.5">
                    <StatusBadge status={p.visible ? "active" : "expired"} label={p.visible ? "노출" : "숨김"} />
                  </td>
                  <td className="px-4 py-3.5">
                    <StatusBadge status={p.active ? "active" : "expired"} label={p.active ? "판매중" : "판매중지"} />
                  </td>
                  <td className="px-4 py-3.5 text-[14px] text-text-sub">{formatSalePeriod(p.saleStartDate, p.saleEndDate)}</td>
                  <td className="px-4 py-3.5 text-[15px] text-text-body">{p.displayOrder}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* 모바일 카드 */}
      <div className="md:hidden flex flex-col gap-3">
        {loading ? (
          <div className="text-center py-8 text-text-sub">로딩 중...</div>
        ) : passes.length === 0 ? (
          <div className="text-center py-8 text-text-sub">등록된 수강권이 없습니다</div>
        ) : (
          passes.map((p) => (
            <div
              key={p.id}
              className="rounded-[18px] border border-border bg-white p-4 flex items-center justify-between cursor-pointer"
              onClick={() => openEditModal(p)}
            >
              <div className="flex-1 min-w-0">
                <p className="text-[15px] font-semibold text-text-title">{p.name}</p>
                <p className="text-[13px] text-text-sub mt-0.5">
                  {CATEGORY_LABELS[p.category || ""] || "-"} | {Number(p.price).toLocaleString()}원 | {p.unlimited ? "무제한" : `${p.totalCount}회`}
                </p>
              </div>
              <div className="flex items-center gap-2 shrink-0 ml-3">
                <StatusBadge status={p.active ? "active" : "expired"} label={p.active ? "판매중" : "중지"} />
              </div>
            </div>
          ))
        )}
      </div>

      {/* 등록/수정 모달 */}
      {modalMode && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={closeModal}>
          <div
            className="bg-white rounded-[18px] w-full max-w-[560px] max-h-[85vh] flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            {/* 헤더 */}
            <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-border shrink-0">
              <h2 className="text-[20px] font-bold text-text-title">
                {modalMode === "create" ? "수강권 등록" : "수강권 수정"}
              </h2>
              <button onClick={closeModal}>
                <X className="h-5 w-5 text-text-sub" />
              </button>
            </div>

            {/* 폼 */}
            <div className="flex-1 overflow-y-auto px-6 py-4">
              <div className="flex flex-col gap-4">
                {/* 이름 */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-[13px] font-semibold text-text-title">이름 *</label>
                  <input
                    type="text"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="예: 개인 12회권"
                    className={inputCls}
                  />
                </div>

                {/* 카테고리 */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-[13px] font-semibold text-text-title">카테고리</label>
                  <select
                    value={form.category}
                    onChange={(e) => setForm({ ...form, category: e.target.value })}
                    className={inputCls}
                  >
                    <option value="PERSONAL">개인</option>
                    <option value="GROUP">그룹</option>
                    <option value="UNLIMITED">무제한</option>
                  </select>
                </div>

                {/* 가격 + 유효기간 */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">가격 (원) *</label>
                    <input
                      type="number"
                      value={form.price}
                      onChange={(e) => setForm({ ...form, price: e.target.value })}
                      placeholder="250000"
                      className={inputCls}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">유효기간 (일) *</label>
                    <input
                      type="number"
                      value={form.validityDays}
                      onChange={(e) => setForm({ ...form, validityDays: e.target.value })}
                      placeholder="90"
                      className={inputCls}
                    />
                  </div>
                </div>

                {/* 무제한 체크 */}
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.unlimited}
                    onChange={(e) =>
                      setForm({ ...form, unlimited: e.target.checked, totalCount: e.target.checked ? "" : form.totalCount })
                    }
                    className="w-4 h-4 accent-[var(--color-pilates)]"
                  />
                  <span className="text-[14px] text-text-body">무제한 여부</span>
                </label>

                {/* 횟수 + 월 최대 */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">횟수</label>
                    <input
                      type="number"
                      value={form.totalCount}
                      onChange={(e) => setForm({ ...form, totalCount: e.target.value })}
                      placeholder="12"
                      disabled={form.unlimited}
                      className={`${inputCls} ${form.unlimited ? "bg-gray-100 text-text-sub" : ""}`}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">월 최대 이용 횟수</label>
                    <input
                      type="number"
                      value={form.monthlyLimit}
                      onChange={(e) => setForm({ ...form, monthlyLimit: e.target.value })}
                      placeholder="30"
                      disabled={!form.unlimited}
                      className={`${inputCls} ${!form.unlimited ? "bg-gray-100 text-text-sub" : ""}`}
                    />
                  </div>
                </div>

                {/* 표시 순서 + 노출 */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">표시 순서</label>
                    <input
                      type="number"
                      value={form.displayOrder}
                      onChange={(e) => setForm({ ...form, displayOrder: e.target.value })}
                      className={inputCls}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">노출 여부</label>
                    <label className="flex items-center gap-2 cursor-pointer mt-2">
                      <input
                        type="checkbox"
                        checked={form.visible}
                        onChange={(e) => setForm({ ...form, visible: e.target.checked })}
                        className="w-4 h-4 accent-[var(--color-pilates)]"
                      />
                      <span className="text-[14px] text-text-body">{form.visible ? "노출" : "숨김"}</span>
                    </label>
                  </div>
                </div>

                {/* 판매 기간 */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">판매 시작일</label>
                    <input
                      type="date"
                      value={form.saleStartDate}
                      onChange={(e) => setForm({ ...form, saleStartDate: e.target.value })}
                      className={inputCls}
                    />
                    <span className="text-[11px] text-text-sub">비워두면 즉시</span>
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">판매 종료일</label>
                    <input
                      type="date"
                      value={form.saleEndDate}
                      onChange={(e) => setForm({ ...form, saleEndDate: e.target.value })}
                      className={inputCls}
                    />
                    <span className="text-[11px] text-text-sub">비워두면 무기한</span>
                  </div>
                </div>

                {/* 상품 설명 */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-[13px] font-semibold text-text-title">상품 설명</label>
                  <textarea
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    placeholder="주 2~3회 추천"
                    rows={3}
                    className={`${inputCls} resize-none`}
                  />
                </div>

                {/* 수업 유형 */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-[13px] font-semibold text-text-title">수업 유형 *</label>
                  {lessonTypes.length === 0 ? (
                    <p className="text-[13px] text-text-sub">수업 유형을 불러오는 중...</p>
                  ) : (
                    <div className="flex flex-wrap gap-2">
                      {lessonTypes.map((lt) => (
                        <label
                          key={lt.id}
                          className={`flex items-center gap-1.5 rounded-[8px] border px-3 py-2 cursor-pointer text-[13px] transition-colors ${
                            form.lessonTypeIds.includes(lt.id)
                              ? "border-pilates bg-pilates-light/30 text-pilates-dark font-semibold"
                              : "border-border text-text-body hover:border-pilates/50"
                          }`}
                        >
                          <input
                            type="checkbox"
                            checked={form.lessonTypeIds.includes(lt.id)}
                            onChange={() => toggleLessonType(lt.id)}
                            className="w-3.5 h-3.5 accent-[var(--color-pilates)]"
                          />
                          {lt.name}
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* 하단 버튼 */}
            <div className="px-6 py-4 border-t border-border shrink-0 flex items-center gap-3">
              {modalMode === "create" ? (
                <button
                  onClick={handleSubmit}
                  disabled={saving}
                  className="flex-1 rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-3 text-[15px] font-semibold text-text-title transition-colors disabled:opacity-40"
                >
                  {saving ? "등록 중..." : "등록하기"}
                </button>
              ) : (
                <>
                  <button
                    onClick={handleSubmit}
                    disabled={saving}
                    className="flex-1 rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-3 text-[15px] font-semibold text-text-title transition-colors disabled:opacity-40"
                  >
                    {saving ? "저장 중..." : "저장"}
                  </button>
                  <button
                    onClick={handleToggleActive}
                    disabled={saving}
                    className={`rounded-[8px] border px-4 py-3 text-[15px] font-semibold transition-colors disabled:opacity-40 ${
                      editTarget?.active
                        ? "border-[#E76F51] text-[#E76F51] hover:bg-[#FDECEA]"
                        : "border-[#4CAF50] text-[#4CAF50] hover:bg-[#E8F5E9]"
                    }`}
                  >
                    {editTarget?.active ? "판매 중지" : "판매 재개"}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
