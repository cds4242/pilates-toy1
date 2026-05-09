"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { useAuthStore } from "@/lib/store/auth-store";
import { authApi } from "@/lib/api/auth";

export default function AdminLoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const inputCls =
    "border border-[#DDDDDD] rounded-[8px] px-3.5 py-3.5 text-[15px] text-text-title placeholder:text-text-sub outline-none focus:border-pilates transition-colors w-full";

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authApi.adminLogin(loginId, password);
      login(res.accessToken, res.refreshToken, {
        id: String(res.adminId),
        role: res.role,
        name: "관리자",
      });
      toast.success("로그인 성공");
      router.push(res.role === "INSTRUCTOR" ? "/instructor/schedule" : "/dashboard");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-bg-section flex items-center justify-center p-6">
      <div className="flex max-w-[900px] w-full min-h-[500px] bg-white rounded-[18px] overflow-hidden shadow-[0_4px_24px_rgba(0,0,0,0.06)] flex-col md:flex-row">
        {/* 히어로 */}
        <div className="flex-1 bg-gradient-to-br from-pilates to-pilates-dark p-10 md:p-[60px] flex flex-col justify-center text-white">
          <div className="rounded-[18px] h-[160px] bg-white/15 flex items-center justify-center text-[15px] font-semibold mb-6">
            학원 사진 영역
          </div>
          <h1 className="text-[28px] md:text-[28px] font-bold mb-4 leading-tight">
            필라테스 OO점<br />관리 시스템
          </h1>
          <p className="text-[15px] opacity-90 leading-relaxed">
            회원, 강사, 시간표, 매출을<br />한곳에서 효율적으로 관리하세요.
          </p>
        </div>

        {/* 폼 */}
        <div className="flex-1 p-6 md:p-[60px] flex flex-col justify-center">
          <span className="inline-block bg-bg-section text-text-body text-[13px] font-semibold px-3 py-1 rounded-[20px] mb-6 w-fit">
            관리자 전용
          </span>
          <h2 className="text-[22px] font-bold text-text-title mb-8">로그인</h2>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-[13px] font-semibold text-text-title">아이디</label>
              <input
                type="text"
                placeholder="admin"
                autoComplete="username"
                value={loginId}
                onChange={(e) => setLoginId(e.target.value)}
                className={inputCls}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-[13px] font-semibold text-text-title">비밀번호</label>
              <input
                type="password"
                placeholder="비밀번호"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={inputCls}
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="bg-text-title hover:bg-[#444] active:scale-[0.99] text-white rounded-[8px] py-4 text-[16px] font-semibold transition-all w-full mt-4 disabled:opacity-60"
            >
              {loading ? "로그인 중..." : "로그인"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
