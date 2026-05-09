"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api/client";
import { toast } from "sonner";

interface Setting {
  id: number; key: string; value: string; description: string; updatedAt: string;
}

export default function AdminSettingsPage() {
  const [settings, setSettings] = useState<Setting[]>([]);
  const [loading, setLoading] = useState(true);
  const [editValues, setEditValues] = useState<Record<string, string>>({});

  useEffect(() => {
    async function load() {
      try {
        const res = await api<{ settings: Setting[] }>("get", "/api/admin/settings");
        setSettings(res.settings);
        const vals: Record<string, string> = {};
        res.settings.forEach(s => { vals[s.key] = s.value; });
        setEditValues(vals);
      } catch { /* empty */ }
      finally { setLoading(false); }
    }
    load();
  }, []);

  const handleSave = async () => {
    try {
      await api("patch", "/api/admin/settings", { settings: editValues });
      toast.success("설정이 저장되었습니다");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "저장 실패");
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-bold text-text-title">학원 설정</h1>
        <button onClick={handleSave} className="rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-2.5 text-[13px] font-semibold text-text-title transition-colors">
          저장
        </button>
      </div>

      <div className="rounded-[18px] border border-border bg-white p-6">
        {loading ? <p className="text-text-sub">로딩 중...</p> : (
          <div className="flex flex-col gap-4">
            {settings.map(s => (
              <div key={s.id} className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-text-title">{s.description || s.key}</label>
                <input
                  type="text"
                  value={editValues[s.key] || ""}
                  onChange={(e) => setEditValues(prev => ({ ...prev, [s.key]: e.target.value }))}
                  className="border border-[#DDDDDD] rounded-[8px] px-3.5 py-2.5 text-[15px] outline-none focus:border-pilates transition-colors"
                />
                <p className="text-[11px] text-text-sub">키: {s.key}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
