"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { useAuthStore } from "@/lib/store/auth-store";
import { authApi } from "@/lib/api/auth";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const inputCls =
    "border border-[#DDDDDD] rounded-[8px] px-3.5 py-3.5 text-[15px] text-text-title placeholder:text-text-sub outline-none focus:border-pilates transition-colors w-full";

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authApi.login(phone.replace(/-/g, ""), password);
      const payload = JSON.parse(atob(res.accessToken.split(".")[1]));
      login(res.accessToken, res.refreshToken, {
        id: payload.sub,
        role: payload.role || "MEMBER",
        name: "회원",
      });
      toast.success("로그인 성공");
      router.push("/home");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white flex flex-col justify-center px-6">
      {/* 스튜디오 이미지 */}
      <div className="mb-6">
        <img src="/studio.jpg" alt="학원 사진" className="w-full h-[160px] object-cover rounded-[18px]" />
      </div>

      {/* 헤더 */}
      <div className="text-center mb-12">
        <h1 className="text-[26px] font-bold text-text-title mb-2">
          필라테스 OO점
        </h1>
        <p className="text-[15px] text-text-body">회원 로그인</p>
      </div>

      {/* 폼 */}
      <form onSubmit={handleSubmit} className="flex flex-col gap-4 mb-6">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="phone" className="text-[13px] font-semibold text-text-title">
            휴대폰 번호
          </label>
          <input type="tel" id="phone" placeholder="010-0000-0000" autoComplete="tel"
            value={phone} onChange={(e) => setPhone(e.target.value)} className={inputCls} />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="password" className="text-[13px] font-semibold text-text-title">
            비밀번호
          </label>
          <input type="password" id="password" placeholder="비밀번호를 입력하세요" autoComplete="current-password"
            value={password} onChange={(e) => setPassword(e.target.value)} className={inputCls} />
        </div>
        <button type="submit" disabled={loading}
          className="bg-pilates hover:bg-pilates-dark active:scale-[0.99] text-text-title rounded-[8px] py-4 text-[16px] font-semibold transition-all w-full disabled:opacity-60">
          {loading ? "로그인 중..." : "로그인"}
        </button>
      </form>

      {/* 링크 */}
      <div className="flex justify-center gap-6">
        <Link href="/signup" className="text-[15px] text-pilates-dark hover:underline">회원가입</Link>
        <button onClick={() => { alert("비밀번호 재설정은 SMS 인증 후 가능합니다.\n회원가입 시 등록한 휴대폰 번호로 인증해주세요."); router.push("/signup"); }} className="text-[15px] text-text-body hover:underline">비밀번호 찾기</button>
      </div>

      {/* 관리자 링크 */}
      <div className="text-center mt-12">
        <Link href="/admin-login" className="text-[13px] text-text-sub hover:text-text-body transition-colors">
          관리자 로그인 →
        </Link>
      </div>
    </div>
  );
}
