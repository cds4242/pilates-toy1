"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { ChevronLeft, Camera } from "lucide-react";
import { useAuthStore } from "@/lib/store/auth-store";
import { authApi } from "@/lib/api/auth";

export default function SignupPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  const [step, setStep] = useState<"phone" | "verify" | "info">("phone");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [verifiedToken, setVerifiedToken] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [gender, setGender] = useState("FEMALE");
  const [loading, setLoading] = useState(false);

  const handleRequestSms = async () => {
    setLoading(true);
    try {
      const normalized = phone.replace(/-/g, "");
      await authApi.requestSms(normalized);
      // 개발 모드: 인증번호 자동 입력
      try {
        const { api: callApi } = await import("@/lib/api/client");
        const res = await callApi<{ code: string }>("get", `/api/test/sms-code/${normalized}`);
        if (res.code) { setCode(res.code); toast.success(`인증번호: ${res.code} (자동 입력됨)`); }
        else { toast.success("인증번호가 발송되었습니다."); }
      } catch { toast.success("인증번호가 발송되었습니다."); }
      setStep("verify");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "발송 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async () => {
    setLoading(true);
    try {
      const res = await authApi.verifySms(phone.replace(/-/g, ""), code);
      setVerifiedToken(res.verifiedToken);
      toast.success("인증 완료");
      setStep("info");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "인증 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleSignup = async () => {
    if (password !== passwordConfirm) {
      toast.error("비밀번호가 일치하지 않습니다.");
      return;
    }
    setLoading(true);
    try {
      const res = await authApi.signup({ verifiedToken, name, password, gender });
      const payload = JSON.parse(atob(res.accessToken.split(".")[1]));
      login(res.accessToken, res.refreshToken, { id: payload.sub, role: "MEMBER", name });
      toast.success("가입이 완료되었습니다!");
      router.push("/home");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "가입 실패");
    } finally {
      setLoading(false);
    }
  };

  const inputCls =
    "border border-[#DDDDDD] rounded-[8px] px-3.5 py-3.5 text-[15px] text-[var(--color-text-title)] placeholder:text-[var(--color-text-sub)] outline-none focus:border-[var(--color-pilates)] transition-colors w-full";

  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center gap-4 border-b border-[var(--color-border)]">
        <button onClick={() => router.back()} className="text-[var(--color-text-title)]">
          <ChevronLeft className="h-6 w-6" />
        </button>
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">회원가입</h1>
      </header>

      <main className="p-6">
        {step === "info" && (
          <>
            {/* 프로필 사진 업로드 */}
            <div className="flex flex-col items-center gap-3 mb-8">
              <div className="w-24 h-24 rounded-full bg-[var(--color-pilates-light)] flex items-center justify-center cursor-pointer hover:bg-[var(--color-pilates)] transition-colors group">
                <Camera className="h-9 w-9 text-[var(--color-pilates-dark)] group-hover:text-white" />
              </div>
              <span className="text-[13px] text-[var(--color-text-sub)]">프로필 사진 등록 (선택)</span>
            </div>

            {/* 가입 폼 */}
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-[var(--color-text-title)]">이름</label>
                <input type="text" placeholder="이름을 입력하세요" value={name} onChange={(e) => setName(e.target.value)} className={inputCls} />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-[var(--color-text-title)]">비밀번호</label>
                <input type="password" placeholder="비밀번호를 입력하세요" value={password} onChange={(e) => setPassword(e.target.value)} className={inputCls} autoComplete="new-password" />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-[var(--color-text-title)]">비밀번호 확인</label>
                <input type="password" placeholder="비밀번호를 다시 입력하세요" value={passwordConfirm} onChange={(e) => setPasswordConfirm(e.target.value)} className={inputCls} autoComplete="new-password" />
              </div>

              {/* 성별 */}
              <div className="flex flex-col gap-1.5">
                <span className="text-[13px] font-semibold text-[var(--color-text-title)]">성별</span>
                <div className="flex gap-3">
                  {[
                    { value: "MALE", label: "남성" },
                    { value: "FEMALE", label: "여성" },
                  ].map((opt) => (
                    <button
                      key={opt.value}
                      type="button"
                      onClick={() => setGender(opt.value)}
                      className={`flex-1 py-3 rounded-[8px] text-[15px] font-medium border transition-all ${
                        gender === opt.value
                          ? "border-[var(--color-pilates)] bg-[var(--color-pilates-light)] text-[var(--color-text-title)] font-semibold"
                          : "border-[#DDDDDD] text-[var(--color-text-body)]"
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* 생년월일 */}
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-[var(--color-text-title)]">생년월일</label>
                <input type="date" className={inputCls} />
              </div>

              <button
                onClick={handleSignup}
                disabled={loading || !name || password.length < 8}
                className="bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] active:scale-[0.99] text-[var(--color-text-title)] rounded-[8px] py-4 text-[16px] font-semibold transition-all w-full mt-4 disabled:opacity-60"
              >
                {loading ? "가입 중..." : "가입하기"}
              </button>
            </div>

            <p className="text-center mt-6 pb-8 text-[15px] text-[var(--color-text-body)]">
              이미 계정이 있으신가요?{" "}
              <Link href="/login" className="text-[var(--color-pilates-dark)] font-semibold hover:underline">
                로그인
              </Link>
            </p>
          </>
        )}

        {step === "phone" && (
          <div className="mt-8 flex flex-col gap-4">
            <p className="text-[15px] text-[var(--color-text-body)] mb-4">
              휴대폰 번호를 인증하면 가입할 수 있습니다.
            </p>
            <div className="flex flex-col gap-1.5">
              <label className="text-[13px] font-semibold text-[var(--color-text-title)]">휴대폰 번호</label>
              <input type="tel" placeholder="01012345678" value={phone} onChange={(e) => setPhone(e.target.value)} className={inputCls} />
            </div>
            <button
              onClick={handleRequestSms}
              disabled={loading || phone.length < 10}
              className="bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] text-[var(--color-text-title)] rounded-[8px] py-4 text-[16px] font-semibold transition-all w-full disabled:opacity-60"
            >
              {loading ? "발송 중..." : "인증번호 받기"}
            </button>
          </div>
        )}

        {step === "verify" && (
          <div className="mt-8 flex flex-col gap-4">
            <p className="text-[15px] text-[var(--color-text-body)] mb-4">
              {phone}으로 발송된 인증번호 6자리를 입력하세요.
            </p>
            <div className="flex flex-col gap-1.5">
              <label className="text-[13px] font-semibold text-[var(--color-text-title)]">인증번호</label>
              <input type="text" placeholder="123456" value={code} onChange={(e) => setCode(e.target.value)} maxLength={6} className={inputCls} />
            </div>
            <button
              onClick={handleVerify}
              disabled={loading || code.length < 6}
              className="bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] text-[var(--color-text-title)] rounded-[8px] py-4 text-[16px] font-semibold transition-all w-full disabled:opacity-60"
            >
              {loading ? "확인 중..." : "인증 확인"}
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
