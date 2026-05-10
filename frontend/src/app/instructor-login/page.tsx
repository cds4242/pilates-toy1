"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { Eye, EyeOff } from "lucide-react";
import { useAuthStore } from "@/lib/store/auth-store";
import { authApi } from "@/lib/api/auth";

export default function InstructorLoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const inputCls =
    "border border-[#DDDDDD] rounded-[8px] px-3.5 py-3.5 text-[15px] text-text-title placeholder:text-text-sub outline-none focus:border-instructor transition-colors w-full";

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authApi.adminLogin(loginId, password);
      login(res.accessToken, res.refreshToken, {
        id: String(res.instructorId ?? res.adminId),
        role: res.role,
        name: loginId,
      });
      toast.success("로그인 성공");
      router.push("/instructor/schedule");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-instructor-light">
      <div className="max-w-[560px] mx-auto min-h-screen bg-white flex flex-col justify-center px-6">
        {/* 스튜디오 사진 */}
        <div className="mb-6">
          <img src="/studio2.jpg" alt="학원 사진" className="w-full h-[160px] object-cover object-[center_30%] rounded-[18px]" />
        </div>

        {/* 헤더 */}
        <div className="text-center mb-12">
          <span className="inline-block bg-instructor text-white text-[13px] font-semibold px-3 py-1 rounded-[20px] mb-4">
            강사용
          </span>
          <h1 className="text-[26px] font-bold text-text-title mb-2">
            필라테스 OO점
          </h1>
          <p className="text-[15px] text-text-body">강사 로그인</p>
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4 mb-6">
          <div className="flex flex-col gap-1.5">
            <label className="text-[13px] font-semibold text-text-title">아이디</label>
            <input type="text" placeholder="강사 아이디" autoComplete="username"
              value={loginId} onChange={(e) => setLoginId(e.target.value)} className={inputCls} />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-[13px] font-semibold text-text-title">비밀번호</label>
            <div className="relative">
              <input type={showPassword ? "text" : "password"} placeholder="비밀번호를 입력하세요" autoComplete="current-password"
                value={password} onChange={(e) => setPassword(e.target.value)} className={inputCls} />
              <button type="button" onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-sub)] hover:text-[var(--color-text-body)]">
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>
          <button type="submit" disabled={loading || !loginId || !password}
            className="bg-instructor hover:bg-[#6A7DC2] active:scale-[0.99] text-white rounded-[8px] py-4 text-[16px] font-semibold transition-all w-full disabled:opacity-60">
            {loading ? "로그인 중..." : "로그인"}
          </button>
        </form>

        {/* 링크 */}
        <div className="flex justify-center gap-6">
          <Link href="/login" className="text-[15px] text-text-sub hover:underline">
            회원 로그인
          </Link>
          <Link href="/admin-login" className="text-[15px] text-text-sub hover:underline">
            관리자 로그인
          </Link>
        </div>
      </div>
    </div>
  );
}
