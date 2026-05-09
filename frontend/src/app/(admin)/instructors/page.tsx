"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api/client";
import { StatusBadge } from "@/components/design/StatusBadge";

interface Instructor {
  id: number; name: string; phone: string | null; status: string; publicId: string; createdAt: string;
}

export default function AdminInstructorsPage() {
  const [instructors, setInstructors] = useState<Instructor[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const data = await api<Instructor[]>("get", "/api/admin/instructors");
        setInstructors(data);
      } catch { /* empty */ }
      finally { setLoading(false); }
    }
    load();
  }, []);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-bold text-text-title">강사 관리</h1>
      </div>

      <div className="rounded-[18px] border border-border bg-white overflow-hidden">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-bg-section border-b border-border">
              {["프로필","이름","상태","등록일"].map(h=>(
                <th key={h} className="text-left px-4 py-3.5 text-[13px] font-semibold text-text-sub">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? <tr><td colSpan={4} className="text-center py-8 text-text-sub">로딩 중...</td></tr>
            : instructors.map(ins=>(
              <tr key={ins.id} className="border-b border-border last:border-0 hover:bg-bg-section transition-colors">
                <td className="px-4 py-3.5"><div className="w-8 h-8 rounded-full bg-instructor-light flex items-center justify-center text-[13px] font-bold text-instructor">{ins.name.charAt(0)}</div></td>
                <td className="px-4 py-3.5 text-[15px] text-text-title font-semibold">{ins.name}</td>
                <td className="px-4 py-3.5"><StatusBadge status={ins.status==="ACTIVE"?"active":"expired"} label={ins.status==="ACTIVE"?"활성":"비활성"}/></td>
                <td className="px-4 py-3.5 text-[13px] text-text-sub">{ins.createdAt?.slice(0,10)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
