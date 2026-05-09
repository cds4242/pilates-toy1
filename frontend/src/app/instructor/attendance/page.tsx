"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, MessageSquare } from "lucide-react";
import { toast } from "sonner";

interface MemberItem {
  id: number;
  name: string;
  phone: string;
  memo?: string;
  status: "PENDING" | "ATTENDED" | "LATE" | "ABSENT";
}

// 시연용 더미 데이터 (실제는 API 연동)
const dummyMembers: MemberItem[] = [
  { id: 1, name: "김민지", phone: "010-****-5678", memo: "허리 주의", status: "PENDING" },
  { id: 2, name: "정하윤", phone: "010-****-9012", status: "PENDING" },
  { id: 3, name: "박소윤", phone: "010-****-7890", status: "PENDING" },
  { id: 4, name: "최유진", phone: "010-****-8901", status: "PENDING" },
  { id: 5, name: "이서연", phone: "010-****-6789", status: "PENDING" },
];

type AttendanceStatus = "ATTENDED" | "LATE" | "ABSENT";

export default function InstructorAttendancePage() {
  const router = useRouter();
  const [members, setMembers] = useState(dummyMembers);

  const setStatus = (id: number, status: AttendanceStatus) => {
    setMembers((prev) => prev.map((m) => m.id === id ? { ...m, status } : m));
  };

  const statusButtons: { value: AttendanceStatus; label: string; colors: string; selected: string }[] = [
    { value: "ATTENDED", label: "출석", colors: "bg-[var(--color-bg-section)] text-[var(--color-text-sub)]", selected: "bg-[#E8F5E9] text-[#4CAF50]" },
    { value: "LATE", label: "지각", colors: "bg-[var(--color-bg-section)] text-[var(--color-text-sub)]", selected: "bg-[#FFF3E0] text-[#F4A261]" },
    { value: "ABSENT", label: "결석", colors: "bg-[var(--color-bg-section)] text-[var(--color-text-sub)]", selected: "bg-[#FDECEA] text-[#E76F51]" },
  ];

  const handleComplete = () => {
    const pending = members.filter((m) => m.status === "PENDING").length;
    if (pending > 0) {
      toast.error(`${pending}명의 출석 상태를 선택해주세요.`);
      return;
    }
    toast.success("출석 체크가 완료되었습니다.");
    router.back();
  };

  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white pb-24">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center gap-4 border-b border-[var(--color-border)]">
        <button onClick={() => router.back()} className="text-[var(--color-text-title)]">
          <ChevronLeft className="h-6 w-6" />
        </button>
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">출석 체크</h1>
      </header>

      {/* 수업 정보 */}
      <div className="bg-[var(--color-bg-section)] px-6 py-4 border-b border-[var(--color-border)] text-[13px] text-[var(--color-text-body)]">
        2026.05.09 (토) 09:00 - 09:50 그룹 필라테스
      </div>

      {/* 회원 리스트 */}
      <div className="p-6 flex flex-col gap-3">
        {members.map((m) => (
          <div key={m.id} className="rounded-[18px] border border-[var(--color-border)] p-4 flex items-center gap-3">
            {/* 아바타 */}
            <div className="w-9 h-9 rounded-full bg-[var(--color-pilates-light)] flex items-center justify-center text-[14px] font-bold text-[var(--color-pilates-dark)] shrink-0">
              {m.name.charAt(0)}
            </div>

            {/* 정보 */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-[15px] font-semibold text-[var(--color-text-title)]">{m.name}</span>
                {m.memo && (
                  <span className="flex items-center gap-0.5 text-[11px] font-semibold text-[var(--color-instructor)]">
                    <MessageSquare className="h-3.5 w-3.5" />{m.memo}
                  </span>
                )}
              </div>
              <p className="text-[13px] text-[var(--color-text-sub)]">{m.phone}</p>
            </div>

            {/* 출석 버튼 */}
            <div className="flex gap-1 shrink-0">
              {statusButtons.map((btn) => (
                <button
                  key={btn.value}
                  onClick={() => setStatus(m.id, btn.value)}
                  className={`rounded-[6px] px-3 py-2 text-[13px] font-semibold transition-colors ${
                    m.status === btn.value ? btn.selected : btn.colors
                  }`}
                >
                  {btn.label}
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* 하단 완료 버튼 */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-[var(--color-border)] p-4">
        <div className="max-w-[480px] mx-auto">
          <button
            onClick={handleComplete}
            className="w-full bg-[var(--color-instructor)] hover:bg-[#6A7DC2] text-white rounded-[8px] py-4 text-[16px] font-semibold transition-colors"
          >
            출석 체크 완료
          </button>
        </div>
      </div>
    </div>
  );
}
