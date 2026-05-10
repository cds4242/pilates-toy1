"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api/client";
import { toast } from "sonner";

interface Setting {
  id: number; key: string; value: string; description: string; updatedAt: string;
}

interface FieldConfig {
  key: string;
  unit: string;
  type: "number" | "text";
  helpText?: string;
}

interface GroupConfig {
  title: string;
  fields: FieldConfig[];
}

const SETTING_GROUPS: GroupConfig[] = [
  {
    title: "예약/취소 설정",
    fields: [
      { key: "CANCEL_DEADLINE_HOURS", unit: "시간", type: "number", helpText: "수업 시작 N시간 전까지 무료 취소 가능합니다" },
      { key: "NO_SHOW_AUTO_MARK_MINUTES", unit: "분", type: "number", helpText: "수업 종료 후 N분 뒤 미출석 회원을 자동 노쇼 처리합니다" },
    ],
  },
  {
    title: "정기권 설정",
    fields: [
      { key: "UNLIMITED_MONTHLY_LIMIT", unit: "회", type: "number", helpText: "무제한권 회원의 월 최대 예약 가능 횟수입니다" },
      { key: "MEMBERSHIP_EXPIRY_ALERT_DAYS", unit: "일 (콤마 구분)", type: "text", helpText: "만료 D-N일 전에 알림을 보냅니다 (콤마로 여러 날짜 입력)" },
      { key: "MEMBERSHIP_LOW_COUNT_ALERT", unit: "회", type: "number", helpText: "잔여 횟수가 N회 이하일 때 알림을 보냅니다" },
    ],
  },
  {
    title: "수업 설정",
    fields: [
      { key: "DEFAULT_LESSON_DURATION", unit: "분", type: "number", helpText: "수업 1회 기본 시간입니다" },
    ],
  },
  {
    title: "알림 설정",
    fields: [
      { key: "REMINDER_1DAY_HOUR", unit: "시", type: "number", helpText: "전날 리마인더를 보내는 시각입니다 (0~23시)" },
      { key: "REMINDER_SAME_DAY_HOURS", unit: "시간 전", type: "number", helpText: "수업 N시간 전에 당일 리마인더를 보냅니다" },
    ],
  },
];

const ALL_GROUPED_KEYS = new Set(SETTING_GROUPS.flatMap(g => g.fields.map(f => f.key)));

function stripNonNumeric(val: string): string {
  return val.replace(/[^0-9]/g, "");
}

function stripNonCommaNumeric(val: string): string {
  return val.replace(/[^0-9,]/g, "");
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

  const settingMap = new Map(settings.map(s => [s.key, s]));

  const ungroupedSettings = settings.filter(s => !ALL_GROUPED_KEYS.has(s.key));

  const renderField = (fieldConfig: FieldConfig) => {
    const s = settingMap.get(fieldConfig.key);
    if (!s) return null;

    const isNumber = fieldConfig.type === "number";
    const isCommaSeparated = fieldConfig.key === "MEMBERSHIP_EXPIRY_ALERT_DAYS";

    return (
      <div key={s.id} className="flex flex-col gap-1.5">
        <label className="text-[13px] font-semibold text-text-title">
          {s.description || s.key}
        </label>
        <div className="flex items-center gap-2">
          <input
            type={isNumber ? "number" : "text"}
            min={isNumber ? "0" : undefined}
            max={isNumber ? "999" : undefined}
            value={editValues[s.key] || ""}
            onChange={(e) =>
              setEditValues(prev => ({ ...prev, [s.key]: e.target.value }))
            }
            onInput={(e) => {
              const target = e.target as HTMLInputElement;
              if (isNumber) {
                target.value = stripNonNumeric(target.value);
              } else if (isCommaSeparated) {
                target.value = stripNonCommaNumeric(target.value);
              }
            }}
            className="flex-1 border border-[#DDDDDD] rounded-[8px] px-3.5 py-2.5 text-[15px] outline-none focus:border-pilates transition-colors"
          />
          <span className="text-[13px] text-text-sub whitespace-nowrap">
            {fieldConfig.unit}
          </span>
        </div>
        {fieldConfig.helpText && (
          <p className="text-[11px] text-text-sub mt-1">{fieldConfig.helpText}</p>
        )}
      </div>
    );
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-bold text-text-title">학원 설정</h1>
        <button onClick={handleSave} className="rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-2.5 text-[13px] font-semibold text-text-title transition-colors">
          저장
        </button>
      </div>

      {loading ? (
        <div className="rounded-[18px] border border-border bg-white p-6">
          <p className="text-text-sub">로딩 중...</p>
        </div>
      ) : (
        <>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {SETTING_GROUPS.map((group) => (
            <div
              key={group.title}
              className="rounded-[18px] border border-border bg-white p-6"
            >
              <h3 className="text-[16px] font-bold text-text-title mb-1">
                {group.title}
              </h3>
              <hr className="border-border mb-4" />
              <div className="flex flex-col gap-4">
                {group.fields.map(renderField)}
              </div>
            </div>
          ))}

          {ungroupedSettings.length > 0 && (
            <div className="rounded-[18px] border border-border bg-white p-6">
              <h3 className="text-[16px] font-bold text-text-title mb-1">
                기타 설정
              </h3>
              <hr className="border-border mb-4" />
              <div className="flex flex-col gap-4">
                {ungroupedSettings.map(s => (
                  <div key={s.id} className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-semibold text-text-title">
                      {s.description || s.key}
                    </label>
                    <input
                      type="text"
                      value={editValues[s.key] || ""}
                      onChange={(e) =>
                        setEditValues(prev => ({ ...prev, [s.key]: e.target.value }))
                      }
                      className="border border-[#DDDDDD] rounded-[8px] px-3.5 py-2.5 text-[15px] outline-none focus:border-pilates transition-colors"
                    />
                    {/* 기술 키 숨김 */}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
        <div className="flex justify-end mt-6">
          <button onClick={handleSave} className="rounded-[8px] bg-pilates hover:bg-pilates-dark px-6 py-3 text-[15px] font-semibold text-text-title transition-colors">변경사항 저장</button>
        </div>
        </>
      )}
    </div>
  );
}
