"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { toast } from "sonner";
import { authApi } from "@/lib/api/auth";
import { api } from "@/lib/api/client";

export default function ResetPasswordPage() {
  const router = useRouter();
  const [step, setStep] = useState<"phone" | "verify" | "reset">("phone");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [verifiedToken, setVerifiedToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const inputCls = "border border-[#DDDDDD] rounded-[8px] px-3.5 py-3.5 text-[15px] text-text-title placeholder:text-text-sub outline-none focus:border-pilates transition-colors w-full";

  const handleRequestSms = async () => {
    setLoading(true);
    try {
      const normalized = phone.replace(/-/g, "");
      await authApi.requestSms(normalized);
      try {
        const res = await api<{ code: string }>("get", `/api/test/sms-code/${normalized}`);
        if (res.code) { setCode(res.code); toast.success(`인증번호: ${res.code} (자동 입력)`); }
        else { toast.success("인증번호가 발송되었습니다."); }
      } catch { toast.success("인증번호가 발송되었습니다."); }
      setStep("verify");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "발송 실패");
    } finally { setLoading(false); }
  };

  const handleVerify = async () => {
    setLoading(true);
    try {
      const res = await authApi.verifySms(phone.replace(/-/g, ""), code);
      setVerifiedToken(res.verifiedToken);
      toast.success("인증 완료");
      setStep("reset");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "인증 실패");
    } finally { setLoading(false); }
  };

  const handleReset = async () => {
    if (newPassword !== confirmPassword) {
      toast.error("비밀번호가 일치하지 않습니다.");
      return;
    }
    if (newPassword.length < 8) {
      toast.error("비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    setLoading(true);
    try {
      const { api: callApi } = await import("@/lib/api/client");
      await callApi("post", "/api/auth/reset-password", {
        verifiedToken,
        newPassword,
      });
      toast.success("비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.");
      router.push("/login");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "변경 실패");
    } finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen bg-bg-section">
      <div className="max-w-[560px] mx-auto min-h-screen bg-white">
        <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center gap-4 border-b border-border">
          <button onClick={() => router.back()} className="text-text-title"><ChevronLeft className="h-6 w-6" /></button>
          <h1 className="text-[20px] font-bold text-text-title">비밀번호 찾기</h1>
        </header>

        <main className="p-6">
          {step === "phone" && (
            <div className="flex flex-col gap-4 mt-4">
              <p className="text-[15px] text-text-body mb-2">가입 시 등록한 휴대폰 번호로 인증 후 비밀번호를 재설정할 수 있습니다.</p>
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-text-title">휴대폰 번호</label>
                <input type="tel" placeholder="01012345678" value={phone} onChange={(e) => setPhone(e.target.value)} className={inputCls} />
              </div>
              <button onClick={handleRequestSms} disabled={loading || phone.length < 10}
                className="bg-pilates hover:bg-pilates-dark text-text-title rounded-[8px] py-4 text-[16px] font-semibold transition-all disabled:opacity-60">
                {loading ? "발송 중..." : "인증번호 받기"}
              </button>
            </div>
          )}

          {step === "verify" && (
            <div className="flex flex-col gap-4 mt-4">
              <p className="text-[15px] text-text-body mb-2">{phone}으로 발송된 인증번호를 입력하세요.</p>
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-text-title">인증번호</label>
                <input type="text" placeholder="123456" value={code} onChange={(e) => setCode(e.target.value)} maxLength={6} className={inputCls} />
              </div>
              <button onClick={handleVerify} disabled={loading || code.length < 6}
                className="bg-pilates hover:bg-pilates-dark text-text-title rounded-[8px] py-4 text-[16px] font-semibold transition-all disabled:opacity-60">
                {loading ? "확인 중..." : "인증 확인"}
              </button>
            </div>
          )}

          {step === "reset" && (
            <div className="flex flex-col gap-4 mt-4">
              <p className="text-[15px] text-text-body mb-2">새 비밀번호를 입력해주세요.</p>
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-text-title">새 비밀번호</label>
                <input type="password" placeholder="8자 이상" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className={inputCls} />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-text-title">비밀번호 확인</label>
                <input type="password" placeholder="비밀번호 다시 입력" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} className={inputCls} />
              </div>
              <button onClick={handleReset} disabled={loading || newPassword.length < 8}
                className="bg-pilates hover:bg-pilates-dark text-text-title rounded-[8px] py-4 text-[16px] font-semibold transition-all disabled:opacity-60">
                {loading ? "변경 중..." : "비밀번호 변경"}
              </button>
            </div>
          )}

          <div className="text-center mt-8">
            <Link href="/login" className="text-[15px] text-pilates-dark hover:underline">로그인으로 돌아가기</Link>
          </div>
        </main>
      </div>
    </div>
  );
}
