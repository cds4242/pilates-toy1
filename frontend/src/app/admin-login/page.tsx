"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { useAuthStore } from "@/lib/store/auth-store";
import { authApi } from "@/lib/api/auth";

const REMEMBER_KEY = "admin-login-remember-id";

export default function AdminLoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(true);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    try {
      const saved = localStorage.getItem(REMEMBER_KEY);
      if (saved) setLoginId(saved);
    } catch {
      // empty
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return;
    setLoading(true);
    try {
      const res = await authApi.adminLogin(loginId, password);
      login(res.accessToken, res.refreshToken, {
        id: String(res.adminId),
        role: res.role,
        name: "관리자",
      });
      try {
        if (remember) localStorage.setItem(REMEMBER_KEY, loginId);
        else localStorage.removeItem(REMEMBER_KEY);
      } catch {
        // empty
      }
      toast.success("로그인 성공");
      router.push(
        res.role === "INSTRUCTOR" ? "/instructor/schedule" : "/dashboard",
      );
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="relative flex min-h-screen items-center justify-center overflow-hidden p-6"
      style={{
        background:
          "radial-gradient(1200px 600px at 20% -10%, #ECE6D8 0%, transparent 60%), radial-gradient(900px 500px at 110% 110%, #E5E0D5 0%, transparent 55%), #F4F1EA",
      }}
    >
      <span
        className="pointer-events-none absolute -right-24 -top-24 h-[360px] w-[360px] rounded-full opacity-50"
        style={{ background: "#D9CFB4", filter: "blur(60px)" }}
      />
      <span
        className="pointer-events-none absolute -bottom-32 -left-24 h-[320px] w-[320px] rounded-full opacity-50"
        style={{ background: "#CDC6B4", filter: "blur(60px)" }}
      />

      <main
        className="relative z-[1] flex w-full max-w-[960px] overflow-hidden rounded-[28px] bg-white"
        style={{
          minHeight: 540,
          boxShadow:
            "inset 0 1px 0 rgba(255,255,255,0.8), 0 30px 60px -20px rgba(60, 50, 30, 0.18), 0 8px 24px -12px rgba(45,30,30,0.08)",
        }}
      >
        {/* Left hero */}
        <div
          className="relative hidden flex-1 flex-col justify-between overflow-hidden p-12 text-white md:flex"
          style={{
            background:
              "linear-gradient(135deg, #2A2730 0%, #1B1920 60%, #14121C 100%)",
          }}
        >
          <img
            src="/studio3.jpg"
            alt="STUDIO"
            className="pointer-events-none absolute inset-0 h-full w-full object-cover object-center opacity-25"
            style={{ filter: "saturate(0.7)" }}
          />
          <span
            className="pointer-events-none absolute inset-0"
            style={{
              background:
                "linear-gradient(135deg, rgba(20,18,30,0.25) 0%, rgba(20,18,30,0.85) 100%)",
            }}
          />

          {/* Top */}
          <div className="relative z-[2]">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1.5 pl-2.5 text-[11px] font-semibold tracking-[0.04em] backdrop-blur-md"
              style={{ color: "#E0C58E" }}
            >
              <span
                className="h-1.5 w-1.5 rounded-full"
                style={{
                  background: "#C9A96E",
                  boxShadow: "0 0 0 3px rgba(201,169,110,0.25)",
                }}
              />
              관리자전용
            </span>

            <div
              className="mt-10 flex h-12 w-12 items-center justify-center rounded-full"
              style={{
                background: "rgba(201,169,110,0.18)",
                border: "1px solid rgba(201,169,110,0.4)",
              }}
            >
              <span
                className="italic"
                style={{
                  fontFamily: "'Cormorant Garamond', Georgia, serif",
                  fontWeight: 500,
                  fontSize: 24,
                  color: "#E0C58E",
                  lineHeight: 1,
                }}
              >
                A
              </span>
            </div>

            <h1 className="mt-5 text-[32px] font-bold leading-[1.2] tracking-[-0.02em] text-white">
              필라테스{" "}
              <span
                className="italic"
                style={{
                  color: "#E0C58E",
                  fontFamily: "'Cormorant Garamond', Georgia, serif",
                  fontWeight: 500,
                }}
              >
                ·
              </span>{" "}
              OO점
            </h1>
            <p
              className="mt-1.5 text-[12px] font-medium uppercase tracking-[0.18em]"
              style={{ color: "#A39A9E" }}
            >
              Admin Console
            </p>
          </div>

          {/* Bottom — selling points */}
          <div className="relative z-[2] mt-10">
            <div
              className="mb-4 h-px w-12"
              style={{ background: "rgba(201,169,110,0.6)" }}
            />
            <p className="text-[15px] font-medium leading-[1.7] text-white/85">
              회원 · 강사 · 시간표 · 매출까지
              <br />
              한 곳에서 효율적으로 관리하세요.
            </p>
            <div
              className="mt-6 flex items-center gap-3 text-[11px]"
              style={{ color: "rgba(255,255,255,0.55)" }}
            >
              <span
                className="block h-[5px] w-[5px] rotate-45"
                style={{ background: "#C9A96E" }}
              />
              <span
                style={{
                  fontFamily: "'Cormorant Garamond', Georgia, serif",
                  fontStyle: "italic",
                  fontWeight: 500,
                  letterSpacing: "0.1em",
                }}
              >
                manage · grow · operate
              </span>
              <span
                className="block h-[5px] w-[5px] rotate-45"
                style={{ background: "#C9A96E" }}
              />
            </div>
          </div>
        </div>

        {/* Right form */}
        <div className="flex flex-1 flex-col justify-center p-8 md:p-12">
          <div className="mb-6">
            <span
              className="text-[11px] font-semibold uppercase tracking-[0.16em]"
              style={{ color: "#A8884D" }}
            >
              Sign In
            </span>
            <h2 className="mt-1.5 text-[24px] font-bold tracking-[-0.02em] text-[#2A2528]">
              관리자 로그인
            </h2>
            <p className="mt-1 text-[13px] text-[#6B6166]">
              운영 정책과 회원 데이터를 다룹니다. 신원이 인증된 계정만
              접근하세요.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-3.5">
            {/* ID */}
            <div className="group relative rounded-[14px] border border-[#E5DECF] bg-[#FAF6EE] py-2.5 pl-[46px] pr-3.5 transition-all focus-within:border-[#C9A96E] focus-within:bg-white focus-within:shadow-[0_0_0_4px_rgba(201,169,110,0.12)]">
              <span
                className="absolute left-3.5 top-1/2 flex h-[22px] w-[22px] -translate-y-1/2 items-center justify-center"
                style={{ color: "#A8884D" }}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.7}
                  stroke="currentColor"
                  className="h-[18px] w-[18px]"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z"
                  />
                </svg>
              </span>
              <label
                htmlFor="loginId"
                className="block text-[11px] font-semibold uppercase tracking-[0.08em] text-[#A39A9E]"
              >
                관리자 아이디
              </label>
              <input
                type="text"
                id="loginId"
                placeholder="admin"
                autoComplete="username"
                value={loginId}
                onChange={(e) => setLoginId(e.target.value)}
                className="mt-0.5 w-full border-none bg-transparent p-0 text-[15px] font-medium tracking-[-0.01em] text-[#2A2528] outline-none placeholder:font-normal placeholder:text-[#A39A9E]"
              />
            </div>

            {/* Password */}
            <div className="group relative rounded-[14px] border border-[#E5DECF] bg-[#FAF6EE] py-2.5 pl-[46px] pr-12 transition-all focus-within:border-[#C9A96E] focus-within:bg-white focus-within:shadow-[0_0_0_4px_rgba(201,169,110,0.12)]">
              <span
                className="absolute left-3.5 top-1/2 flex h-[22px] w-[22px] -translate-y-1/2 items-center justify-center"
                style={{ color: "#A8884D" }}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.7}
                  stroke="currentColor"
                  className="h-[18px] w-[18px]"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"
                  />
                </svg>
              </span>
              <label
                htmlFor="password"
                className="block text-[11px] font-semibold uppercase tracking-[0.08em] text-[#A39A9E]"
              >
                비밀번호
              </label>
              <input
                type={showPassword ? "text" : "password"}
                id="password"
                placeholder="비밀번호를 입력하세요"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-0.5 w-full border-none bg-transparent p-0 text-[15px] font-medium tracking-[-0.01em] text-[#2A2528] outline-none placeholder:font-normal placeholder:text-[#A39A9E]"
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                aria-label="비밀번호 표시 전환"
                className="absolute right-2.5 top-1/2 flex h-8 w-8 -translate-y-1/2 cursor-pointer items-center justify-center rounded-lg border-none bg-transparent text-[#A39A9E] transition-colors hover:bg-[#FAF6EE] hover:text-[#A8884D]"
              >
                {showPassword ? (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    strokeWidth={1.7}
                    stroke="currentColor"
                    className="h-[18px] w-[18px]"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88"
                    />
                  </svg>
                ) : (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    strokeWidth={1.7}
                    stroke="currentColor"
                    className="h-[18px] w-[18px]"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z"
                    />
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"
                    />
                  </svg>
                )}
              </button>
            </div>

            {/* Remember */}
            <div className="my-1 flex items-center justify-between">
              <label className="inline-flex cursor-pointer select-none items-center gap-2 text-[13px] text-[#6B6166]">
                <input
                  type="checkbox"
                  checked={remember}
                  onChange={(e) => setRemember(e.target.checked)}
                  className="hidden"
                />
                <span
                  className={`flex h-4 w-4 items-center justify-center rounded-[5px] border-[1.5px] transition-all ${
                    remember
                      ? "border-[#C9A96E] bg-[#C9A96E]"
                      : "border-[#E5DECF]"
                  }`}
                >
                  {remember && (
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                      strokeWidth={3}
                      stroke="currentColor"
                      className="h-2.5 w-2.5 text-white"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="m4.5 12.75 6 6 9-13.5"
                      />
                    </svg>
                  )}
                </span>
                자동 로그인
              </label>
              <span className="text-[12px] text-[#A39A9E]">
                보안 환경에서만 사용
              </span>
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={loading || !loginId || !password}
              className="group/btn relative mt-2 w-full cursor-pointer overflow-hidden rounded-[14px] border-none px-4 py-4 text-[15px] font-bold tracking-[0.02em] text-white transition-[transform,box-shadow] disabled:cursor-not-allowed disabled:opacity-60"
              style={{
                background:
                  "linear-gradient(135deg, #4A4651 0%, #2E2B36 60%, #1B1920 100%)",
                boxShadow:
                  "0 8px 18px -6px rgba(40, 35, 50, 0.55), inset 0 0 0 1px rgba(201,169,110,0.25)",
              }}
            >
              {loading ? (
                "로그인 중..."
              ) : (
                <>
                  <span style={{ color: "#E0C58E" }}>관리자 </span>로그인{" "}
                  <span className="ml-1 inline-block transition-transform group-hover/btn:translate-x-1">
                    →
                  </span>
                </>
              )}
            </button>
          </form>

          {/* Foot */}
          <div className="mt-6 flex items-center justify-center gap-3.5 border-t border-[#ECE4E0] pt-5">
            <Link
              href="/login"
              className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1.5 text-[12px] text-[#A39A9E] no-underline transition-all hover:bg-[#FAF6EE] hover:text-[#A8884D]"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.7}
                stroke="currentColor"
                className="h-3.5 w-3.5"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z"
                />
              </svg>
              회원 로그인
            </Link>
            <span className="block h-3.5 w-px bg-[#ECE4E0]" />
            <Link
              href="/instructor-login"
              className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1.5 text-[12px] text-[#A39A9E] no-underline transition-all hover:bg-[#FAF6EE] hover:text-[#A8884D]"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.7}
                stroke="currentColor"
                className="h-3.5 w-3.5"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M9 17.25v1.007a3 3 0 0 1-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0 1 15 18.257V17.25m6-12V15a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 15V5.25m18 0A2.25 2.25 0 0 0 18.75 3H5.25A2.25 2.25 0 0 0 3 5.25m18 0V12a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 12V5.25"
                />
              </svg>
              강사 로그인
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
