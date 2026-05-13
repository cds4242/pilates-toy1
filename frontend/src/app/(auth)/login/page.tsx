"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { useAuthStore } from "@/lib/store/auth-store";
import { authApi } from "@/lib/api/auth";

const REMEMBER_KEY = "login-remember-phone";

const IS_DEMO_MODE = process.env.NEXT_PUBLIC_DEMO_MODE === "true";

type DemoAccount =
  | {
      kind: "member";
      label: string;
      icon: string;
      desc: string;
      phone: string;
      password: string;
      accent: string;
    }
  | {
      kind: "admin" | "instructor";
      label: string;
      icon: string;
      desc: string;
      loginId: string;
      password: string;
      accent: string;
    };

const DEMO_ACCOUNTS: DemoAccount[] = [
  {
    kind: "member",
    label: "회원",
    icon: "👤",
    desc: "12회권 보유 + 예약 3건",
    phone: "01000000001",
    password: "demo1234",
    accent: "#E89BB0",
  },
  {
    kind: "instructor",
    label: "강사",
    icon: "🧘",
    desc: "담당 수업 5건 + 출석 체크",
    loginId: "instructor_demo",
    password: "demo1234",
    accent: "#7C8FD4",
  },
  {
    kind: "admin",
    label: "관리자",
    icon: "⚙️",
    desc: "대시보드 + 회원/매출 관리",
    loginId: "admin_demo",
    password: "demo1234",
    accent: "#C9A96E",
  },
];

function formatPhone(value: string) {
  const nums = value.replace(/\D/g, "").slice(0, 11);
  if (nums.length <= 3) return nums;
  if (nums.length <= 7) return nums.slice(0, 3) + "-" + nums.slice(3);
  return nums.slice(0, 3) + "-" + nums.slice(3, 7) + "-" + nums.slice(7);
}

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(true);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    try {
      const saved = localStorage.getItem(REMEMBER_KEY);
      if (saved) setPhone(formatPhone(saved));
    } catch {
      // empty
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return;
    setLoading(true);
    try {
      const cleanPhone = phone.replace(/-/g, "");
      const res = await authApi.login(cleanPhone, password);
      const payload = JSON.parse(atob(res.accessToken.split(".")[1]));
      login(res.accessToken, res.refreshToken, {
        id: payload.sub,
        role: payload.role || "MEMBER",
        name: "회원",
      });
      try {
        if (remember) localStorage.setItem(REMEMBER_KEY, cleanPhone);
        else localStorage.removeItem(REMEMBER_KEY);
      } catch {
        // empty
      }
      toast.success("로그인 성공");
      router.push("/home");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "로그인에 실패했습니다.";
      if (msg.includes("찾을 수 없") || msg.includes("NOT_FOUND")) {
        toast.error("가입되지 않은 번호입니다. 회원가입을 진행해주세요.");
      } else {
        toast.error(msg);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = async (acc: DemoAccount) => {
    if (loading) return;
    setLoading(true);
    try {
      if (acc.kind === "member") {
        setPhone(formatPhone(acc.phone));
        setPassword(acc.password);
        const res = await authApi.login(acc.phone, acc.password);
        const payload = JSON.parse(atob(res.accessToken.split(".")[1]));
        login(res.accessToken, res.refreshToken, {
          id: payload.sub,
          role: payload.role || "MEMBER",
          name: "김데모",
        });
        toast.success("데모 회원으로 로그인");
        router.push("/home");
      } else {
        const res = await authApi.adminLogin(acc.loginId, acc.password);
        login(res.accessToken, res.refreshToken, {
          id: String(res.adminId),
          role: res.role,
          name: acc.kind === "instructor" ? "박데모" : "데모관리자",
        });
        toast.success(
          acc.kind === "instructor"
            ? "데모 강사로 로그인"
            : "데모 관리자로 로그인",
        );
        router.push(
          res.role === "INSTRUCTOR" ? "/instructor/schedule" : "/dashboard",
        );
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "데모 로그인 실패";
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-6"
      style={{
        background:
          "radial-gradient(1200px 600px at 20% -10%, #FCE6EC 0%, transparent 60%), radial-gradient(900px 500px at 110% 110%, #F5E5DC 0%, transparent 55%), #F7F2EE",
      }}
    >
      <span
        className="pointer-events-none absolute -right-20 -top-20 h-[320px] w-[320px] rounded-full opacity-50"
        style={{ background: "#F8C8D4", filter: "blur(40px)" }}
      />
      <span
        className="pointer-events-none absolute -bottom-[100px] -left-[60px] h-[280px] w-[280px] rounded-full opacity-50"
        style={{ background: "#EBD7C8", filter: "blur(40px)" }}
      />

      <main
        className="relative z-[1] w-full max-w-[432px] rounded-[28px] bg-white px-[22px] pb-7 pt-[22px]"
        style={{
          boxShadow:
            "inset 0 1px 0 rgba(255,255,255,0.8), 0 30px 60px -20px rgba(155, 90, 114, 0.18), 0 8px 24px -12px rgba(45,30,30,0.08)",
        }}
      >
        {/* Hero image */}
        <div
          className="relative mb-7 h-[200px] overflow-hidden rounded-[22px]"
          style={{ background: "linear-gradient(135deg, #F0A0B5, #D88A9E)" }}
        >
          <img
            src={`${process.env.NEXT_PUBLIC_BASE_PATH || ""}/studio.jpg`}
            alt="필라테스 스튜디오"
            className="h-full w-full object-cover"
          />
          <span
            className="pointer-events-none absolute inset-0"
            style={{
              background:
                "linear-gradient(180deg, rgba(0,0,0,0) 40%, rgba(45,20,30,0.35) 100%)",
            }}
          />
          <span
            className="absolute left-4 top-4 z-[2] inline-flex items-center gap-1.5 rounded-full bg-white/95 px-3 py-1.5 pl-2.5 text-[11px] font-semibold tracking-[0.04em] text-[#9B5A72] backdrop-blur-md"
          >
            <span
              className="h-1.5 w-1.5 rounded-full"
              style={{
                background: "#E89BB0",
                boxShadow: "0 0 0 3px rgba(232,155,176,0.25)",
              }}
            />
            OPEN · 평일 07–22시
          </span>
          <span className="absolute bottom-3.5 left-4 z-[2] text-[12px] font-medium uppercase tracking-[0.16em] text-white/90">
            PILATES · STUDIO OO
          </span>
        </div>

        {/* Brand */}
        <div className="mb-7 px-2 text-center">
          <div className="relative mx-auto mb-3.5 flex h-11 w-11 items-center justify-center rounded-full border border-[#FBE9EE] bg-[#FBE9EE] text-[22px] leading-none text-[#9B5A72]">
            <span
              className="italic"
              style={{
                fontFamily: "'Cormorant Garamond', Georgia, serif",
                fontWeight: 500,
              }}
            >
              P
            </span>
            <span
              className="pointer-events-none absolute -inset-[5px] rounded-full"
              style={{ border: "1px dashed rgba(201,125,148,0.35)" }}
            />
          </div>
          <h1 className="mb-1.5 text-[24px] font-bold leading-[1.2] tracking-[-0.02em] text-[#2A2528]">
            필라테스{" "}
            <span
              className="inline-block px-0.5 italic text-[#E89BB0]"
              style={{
                fontFamily: "'Cormorant Garamond', Georgia, serif",
                fontWeight: 500,
              }}
            >
              ·
            </span>{" "}
            OO점
          </h1>
          <div className="text-[13px] font-medium uppercase tracking-[0.16em] text-[#A39A9E]">
            Member Sign In
          </div>
          <div className="mx-auto mt-4 flex w-3/5 items-center gap-2.5 text-[#E89BB0]">
            <span
              className="h-px flex-1"
              style={{
                background:
                  "linear-gradient(90deg, transparent, rgba(201,125,148,0.35), transparent)",
              }}
            />
            <span
              className="block h-[5px] w-[5px] rotate-45"
              style={{ background: "#E89BB0" }}
            />
            <span
              className="h-px flex-1"
              style={{
                background:
                  "linear-gradient(90deg, transparent, rgba(201,125,148,0.35), transparent)",
              }}
            />
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-3.5">
          {/* Phone */}
          <div className="group relative rounded-[14px] border border-[#E8DDD8] bg-[#FBF7F4] py-2.5 pl-[46px] pr-3.5 transition-all focus-within:border-[#E89BB0] focus-within:bg-white focus-within:shadow-[0_0_0_4px_rgba(232,155,176,0.12)]">
            <span className="absolute left-3.5 top-1/2 flex h-[22px] w-[22px] -translate-y-1/2 items-center justify-center text-[#C97D94]">
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
                  d="M10.5 1.5h-.75A2.25 2.25 0 0 0 7.5 3.75v16.5a2.25 2.25 0 0 0 2.25 2.25h7.5a2.25 2.25 0 0 0 2.25-2.25V3.75a2.25 2.25 0 0 0-2.25-2.25h-.75m-6 0v.75a.75.75 0 0 0 .75.75h4.5a.75.75 0 0 0 .75-.75V1.5m-6 0h6"
                />
              </svg>
            </span>
            <label
              htmlFor="phone"
              className="block text-[11px] font-semibold uppercase tracking-[0.08em] text-[#A39A9E]"
            >
              휴대폰 번호
            </label>
            <input
              type="tel"
              id="phone"
              placeholder="010-0000-0000"
              autoComplete="tel"
              inputMode="tel"
              value={phone}
              onChange={(e) => setPhone(formatPhone(e.target.value))}
              className="mt-0.5 w-full border-none bg-transparent p-0 text-[15px] font-medium tracking-[-0.01em] text-[#2A2528] outline-none placeholder:font-normal placeholder:text-[#A39A9E]"
            />
          </div>

          {/* Password */}
          <div className="group relative rounded-[14px] border border-[#E8DDD8] bg-[#FBF7F4] py-2.5 pl-[46px] pr-12 transition-all focus-within:border-[#E89BB0] focus-within:bg-white focus-within:shadow-[0_0_0_4px_rgba(232,155,176,0.12)]">
            <span className="absolute left-3.5 top-1/2 flex h-[22px] w-[22px] -translate-y-1/2 items-center justify-center text-[#C97D94]">
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
              className="absolute right-2.5 top-1/2 flex h-8 w-8 -translate-y-1/2 cursor-pointer items-center justify-center rounded-lg border-none bg-transparent text-[#A39A9E] transition-colors hover:bg-[#FDF5F7] hover:text-[#C97D94]"
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

          {/* Remember + reset */}
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
                    ? "border-[#E89BB0] bg-[#E89BB0]"
                    : "border-[#E8DDD8]"
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
            <Link
              href="/reset-password"
              className="text-[13px] font-medium text-[#6B6166] no-underline hover:text-[#9B5A72]"
            >
              비밀번호 찾기
            </Link>
          </div>

          {/* Primary button */}
          <button
            type="submit"
            disabled={loading || !phone || !password}
            className="group/btn relative mt-1.5 w-full cursor-pointer overflow-hidden rounded-[14px] border-none px-4 py-4 text-[15px] font-bold tracking-[0.02em] text-white transition-[transform,box-shadow] disabled:cursor-not-allowed disabled:opacity-60"
            style={{
              background:
                "linear-gradient(135deg, #F0A8BC 0%, #D88A9E 60%, #C97D94 100%)",
              boxShadow:
                "0 8px 18px -6px rgba(201, 125, 148, 0.5), inset 0 0 0 1px rgba(255,255,255,0.2)",
            }}
          >
            {loading ? (
              "로그인 중..."
            ) : (
              <>
                로그인{" "}
                <span className="ml-1 inline-block transition-transform group-hover/btn:translate-x-1">
                  →
                </span>
              </>
            )}
          </button>
        </form>

        {/* Demo accounts (포트폴리오 시연 모드) */}
        {IS_DEMO_MODE && (
          <div className="mt-6 border-t border-[#ECE4E0] pt-6">
            <div className="mb-3 text-center">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-[#FBE9EE] px-3 py-1 text-[11px] font-semibold tracking-[0.04em] text-[#9B5A72]">
                🎬 데모 시연 모드
              </span>
              <p className="mt-2 text-[12px] text-[#A39A9E]">
                클릭 한 번으로 로그인하여 둘러볼 수 있습니다
              </p>
            </div>
            <div className="grid grid-cols-3 gap-2.5">
              {DEMO_ACCOUNTS.map((acc) => (
                <button
                  key={acc.label}
                  type="button"
                  disabled={loading}
                  onClick={() => handleDemoLogin(acc)}
                  className="group/demo flex flex-col items-start rounded-[12px] border bg-white p-3 text-left transition-all hover:-translate-y-0.5 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-50"
                  style={{
                    borderColor: `${acc.accent}55`,
                  }}
                >
                  <div className="text-[20px] leading-none">{acc.icon}</div>
                  <div
                    className="mt-1.5 text-[13px] font-bold"
                    style={{ color: acc.accent }}
                  >
                    {acc.label}
                  </div>
                  <div className="mt-0.5 text-[10.5px] leading-[1.3] text-[#6B6166]">
                    {acc.desc}
                  </div>
                  <div className="mt-1.5 w-full border-t border-[#ECE4E0] pt-1.5 font-mono text-[10px] leading-[1.4] text-[#A39A9E]">
                    <div className="truncate">
                      {acc.kind === "member" ? "010-0000-0001" : acc.loginId}
                    </div>
                    <div>{acc.password}</div>
                  </div>
                </button>
              ))}
            </div>
            <p className="mt-3 text-center text-[10.5px] text-[#A39A9E]">
              ⚠️ 데모 데이터는 시연용이며 서버 재기동 시 초기화됩니다
            </p>
          </div>
        )}

        {/* Sub links */}
        <div className="mt-[18px] flex items-center justify-center gap-[18px] text-[13px]">
          <span className="text-[#A39A9E]">아직 회원이 아니신가요?</span>
          <Link
            href="/signup"
            className="font-semibold text-[#9B5A72] no-underline hover:text-[#9B5A72]"
          >
            회원가입
          </Link>
        </div>

        {/* Foot */}
        <div className="mt-[22px] flex items-center justify-center gap-3.5 border-t border-[#ECE4E0] pt-[18px]">
          <Link
            href="/instructor-login"
            className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1.5 text-[12px] text-[#A39A9E] no-underline transition-all hover:bg-[#FDF5F7] hover:text-[#9B5A72]"
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
          <span className="block h-3.5 w-px bg-[#ECE4E0]" />
          <Link
            href="/admin-login"
            className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1.5 text-[12px] text-[#A39A9E] no-underline transition-all hover:bg-[#FDF5F7] hover:text-[#9B5A72]"
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
                d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.325.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 0 1 1.37.49l1.296 2.247a1.125 1.125 0 0 1-.26 1.431l-1.003.827c-.293.241-.438.613-.43.992a7.723 7.723 0 0 1 0 .255c-.008.378.137.75.43.991l1.004.827c.424.35.534.955.26 1.43l-1.298 2.247a1.125 1.125 0 0 1-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.47 6.47 0 0 1-.22.128c-.331.183-.581.495-.644.869l-.213 1.281c-.09.543-.56.94-1.11.94h-2.594c-.55 0-1.019-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 0 1-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 0 1-1.369-.49l-1.297-2.247a1.125 1.125 0 0 1 .26-1.431l1.004-.827c.292-.24.437-.613.43-.991a6.932 6.932 0 0 1 0-.255c.007-.38-.138-.751-.43-.992l-1.004-.827a1.125 1.125 0 0 1-.26-1.43l1.297-2.247a1.125 1.125 0 0 1 1.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.086.22-.128.332-.183.582-.495.644-.869l.214-1.28Z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"
              />
            </svg>
            관리자 로그인
          </Link>
        </div>

        {/* Signature */}
        <div
          className="mt-[18px] text-center text-[11px] tracking-[0.1em] text-[#A39A9E]"
          style={{
            fontFamily: "'Cormorant Garamond', Georgia, serif",
            fontStyle: "italic",
            fontWeight: 500,
          }}
        >
          breathe · move · balance
        </div>
      </main>
    </div>
  );
}
