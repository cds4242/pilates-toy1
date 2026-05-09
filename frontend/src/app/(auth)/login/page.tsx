"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { useAuthStore } from "@/lib/store/auth-store";
import { authApi } from "@/lib/api/auth";

type Tab = "member" | "admin";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  const [tab, setTab] = useState<Tab>("member");

  // 회원
  const [phone, setPhone] = useState("");
  const [memberPw, setMemberPw] = useState("");

  // 관리자
  const [loginId, setLoginId] = useState("");
  const [adminPw, setAdminPw] = useState("");

  const [loading, setLoading] = useState(false);

  const handleMemberLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authApi.login(phone.replace(/-/g, ""), memberPw);
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

  const handleAdminLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authApi.adminLogin(loginId, adminPw);
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

  const inputCls =
    "border border-[#DDDDDD] rounded-[8px] px-3.5 py-3.5 text-[15px] text-[var(--color-text-title)] placeholder:text-[var(--color-text-sub)] outline-none focus:border-[var(--color-pilates)] transition-colors w-full";

  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white flex flex-col justify-center px-6">
      {/* 스튜디오 이미지 */}
      <div className="mb-6">
        <div className="bg-gradient-to-br from-[var(--color-pilates)] to-[var(--color-pilates-dark)] rounded-[18px] h-[160px] flex items-center justify-center text-white text-[15px] font-semibold">
          Pilates Studio
        </div>
      </div>

      {/* 헤더 */}
      <div className="text-center mb-8">
        <h1 className="text-[26px] font-bold text-[var(--color-text-title)] mb-2">
          필라테스 OO점
        </h1>
        <p className="text-[15px] text-[var(--color-text-body)]">
          {tab === "member" ? "회원 로그인" : "관리자 로그인"}
        </p>
      </div>

      {/* 탭 */}
      <div className="flex mb-6 border-b border-[var(--color-border)]">
        <button
          onClick={() => setTab("member")}
          className={`flex-1 py-3 text-[15px] font-medium text-center transition-colors ${
            tab === "member"
              ? "text-[var(--color-pilates-dark)] border-b-2 border-[var(--color-pilates)] font-semibold"
              : "text-[var(--color-text-sub)]"
          }`}
        >
          회원
        </button>
        <button
          onClick={() => setTab("admin")}
          className={`flex-1 py-3 text-[15px] font-medium text-center transition-colors ${
            tab === "admin"
              ? "text-[var(--color-text-title)] border-b-2 border-[var(--color-text-title)] font-semibold"
              : "text-[var(--color-text-sub)]"
          }`}
        >
          관리자
        </button>
      </div>

      {/* 회원 폼 */}
      {tab === "member" && (
        <form onSubmit={handleMemberLogin} className="flex flex-col gap-4 mb-6">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="phone" className="text-[13px] font-semibold text-[var(--color-text-title)]">
              휴대폰 번호
            </label>
            <input type="tel" id="phone" placeholder="010-0000-0000" autoComplete="tel"
              value={phone} onChange={(e) => setPhone(e.target.value)} className={inputCls} />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="memberPw" className="text-[13px] font-semibold text-[var(--color-text-title)]">
              비밀번호
            </label>
            <input type="password" id="memberPw" placeholder="비밀번호를 입력하세요" autoComplete="current-password"
              value={memberPw} onChange={(e) => setMemberPw(e.target.value)} className={inputCls} />
          </div>
          <button type="submit" disabled={loading}
            className="bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] active:scale-[0.99] text-[var(--color-text-title)] rounded-[8px] py-4 text-[16px] font-semibold transition-all w-full disabled:opacity-60">
            {loading ? "로그인 중..." : "로그인"}
          </button>
          <div className="flex justify-center gap-6 mt-2">
            <Link href="/signup" className="text-[15px] text-[var(--color-pilates-dark)] hover:underline">회원가입</Link>
            <button type="button" className="text-[15px] text-[var(--color-text-body)] hover:underline">비밀번호 찾기</button>
          </div>
        </form>
      )}

      {/* 관리자 폼 */}
      {tab === "admin" && (
        <form onSubmit={handleAdminLogin} className="flex flex-col gap-4 mb-6">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="loginId" className="text-[13px] font-semibold text-[var(--color-text-title)]">
              아이디
            </label>
            <input type="text" id="loginId" placeholder="관리자 아이디" autoComplete="username"
              value={loginId} onChange={(e) => setLoginId(e.target.value)} className={inputCls} />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="adminPw" className="text-[13px] font-semibold text-[var(--color-text-title)]">
              비밀번호
            </label>
            <input type="password" id="adminPw" placeholder="비밀번호를 입력하세요" autoComplete="current-password"
              value={adminPw} onChange={(e) => setAdminPw(e.target.value)} className={inputCls} />
          </div>
          <button type="submit" disabled={loading}
            className="bg-[var(--color-text-title)] hover:bg-[#444] active:scale-[0.99] text-white rounded-[8px] py-4 text-[16px] font-semibold transition-all w-full disabled:opacity-60">
            {loading ? "로그인 중..." : "관리자 로그인"}
          </button>
        </form>
      )}
    </div>
  );
}
