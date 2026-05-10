"use client";

import { useState } from "react";
import { ChevronLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { toast } from "sonner";

interface NotificationSetting {
  key: string;
  label: string;
  description: string;
  enabled: boolean;
}

export default function NotificationSettingsPage() {
  const router = useRouter();
  const [settings, setSettings] = useState<NotificationSetting[]>([
    { key: "reservation_confirm", label: "예약 확인", description: "수업 예약 완료 시 알림", enabled: true },
    { key: "reservation_cancel", label: "예약 취소", description: "수업 예약 취소 시 알림", enabled: true },
    { key: "reminder_1day", label: "수업 전날 리마인더", description: "수업 하루 전 알림", enabled: true },
    { key: "reminder_1hour", label: "수업 1시간 전 리마인더", description: "수업 1시간 전 알림", enabled: true },
    { key: "membership_expiry", label: "수강권 만료 알림", description: "수강권 만료 7일/3일 전 알림", enabled: true },
    { key: "membership_low", label: "수강권 잔여 부족 알림", description: "잔여 횟수 3회 이하 시 알림", enabled: true },
    { key: "marketing", label: "마케팅 알림", description: "이벤트, 프로모션 안내", enabled: false },
  ]);
  const [saving, setSaving] = useState(false);

  const toggle = (key: string) => {
    setSettings(prev => prev.map(s => s.key === key ? { ...s, enabled: !s.enabled } : s));
  };

  const handleSave = async () => {
    setSaving(true);
    await new Promise(r => setTimeout(r, 800));
    toast.success("알림 설정이 저장되었습니다.");
    setSaving(false);
  };

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white pb-20">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center gap-4 border-b border-[var(--color-border)]">
        <button onClick={() => router.back()} className="text-[var(--color-text-title)]">
          <ChevronLeft className="h-6 w-6" />
        </button>
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">알림 설정</h1>
      </header>

      <main className="p-6 flex flex-col gap-4">
        <p className="text-[14px] text-[var(--color-text-sub)] mb-2">
          받고 싶은 알림을 선택하세요. 변경 후 저장 버튼을 눌러주세요.
        </p>

        {settings.map((s) => (
          <div key={s.key} className="flex items-center justify-between py-3 border-b border-[var(--color-border)]">
            <div className="flex-1">
              <p className="text-[15px] font-semibold text-[var(--color-text-title)]">{s.label}</p>
              <p className="text-[12px] text-[var(--color-text-sub)] mt-0.5">{s.description}</p>
            </div>
            <button
              onClick={() => toggle(s.key)}
              className={`relative w-12 h-7 rounded-full transition-colors ${s.enabled ? "bg-[var(--color-pilates)]" : "bg-gray-300"}`}
            >
              <div className={`absolute top-0.5 w-6 h-6 rounded-full bg-white shadow transition-transform ${s.enabled ? "translate-x-5" : "translate-x-0.5"}`} />
            </button>
          </div>
        ))}

        <button
          onClick={handleSave}
          disabled={saving}
          className="mt-4 w-full rounded-[8px] bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] py-4 text-[16px] font-semibold text-[var(--color-text-title)] transition-colors disabled:opacity-60"
        >
          {saving ? "저장 중..." : "설정 저장"}
        </button>
      </main>

      <MobileTabBar />
    </div>
  );
}
