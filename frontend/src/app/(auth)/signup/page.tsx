"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { ChevronLeft, Camera, Eye, EyeOff } from "lucide-react";

function formatPhone(value: string) {
  const nums = value.replace(/\D/g, "").slice(0, 11);
  if (nums.length <= 3) return nums;
  if (nums.length <= 7) return nums.slice(0, 3) + "-" + nums.slice(3);
  return nums.slice(0, 3) + "-" + nums.slice(3, 7) + "-" + nums.slice(7);
}
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
  const [showPassword, setShowPassword] = useState(false);
  const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);
  const [profileImage, setProfileImage] = useState<File | null>(null);
  const [profilePreview, setProfilePreview] = useState<string | null>(null);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

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

  const handleProfileImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      toast.error("이미지 크기는 5MB 이하만 가능합니다.");
      return;
    }
    setProfileImage(file);
    const reader = new FileReader();
    reader.onloadend = () => setProfilePreview(reader.result as string);
    reader.readAsDataURL(file);
  };

  // 비밀번호 규칙 체크
  const passwordChecks = {
    length: password.length >= 8,
    upper: /[A-Z]/.test(password),
    lower: /[a-z]/.test(password),
    number: /[0-9]/.test(password),
    special: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(password),
  };
  const satisfiedTypes = [passwordChecks.upper, passwordChecks.lower, passwordChecks.number, passwordChecks.special].filter(Boolean).length;
  const passwordValid = passwordChecks.length && satisfiedTypes >= 3;
  const passwordMatch = password === passwordConfirm && passwordConfirm.length > 0;

  const handleSignup = async () => {
    if (!passwordValid) {
      toast.error("비밀번호 규칙을 확인해주세요.");
      return;
    }
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
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleProfileImageChange}
              />
              <div
                onClick={() => fileInputRef.current?.click()}
                className="w-24 h-24 rounded-full bg-[var(--color-pilates-light)] flex items-center justify-center cursor-pointer hover:bg-[var(--color-pilates)] transition-colors group overflow-hidden"
              >
                {profilePreview ? (
                  <img src={profilePreview} alt="프로필" className="w-full h-full object-cover" />
                ) : (
                  <Camera className="h-9 w-9 text-[var(--color-pilates-dark)] group-hover:text-white" />
                )}
              </div>
              <span className="text-[13px] text-[var(--color-text-sub)]">
                {profileImage ? profileImage.name : "프로필 사진 등록 (선택)"}
              </span>
            </div>

            {/* 가입 폼 */}
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-[var(--color-text-title)]">이름</label>
                <input type="text" placeholder="이름을 입력하세요" value={name} onChange={(e) => setName(e.target.value)} className={inputCls} />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-[var(--color-text-title)]">비밀번호</label>
                <div className="relative">
                  <input type={showPassword ? "text" : "password"} placeholder="비밀번호를 입력하세요" value={password} onChange={(e) => setPassword(e.target.value)} className={inputCls} autoComplete="new-password" />
                  <button type="button" onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-sub)] hover:text-[var(--color-text-body)]">
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {password.length > 0 && (
                  <div className="mt-1 flex flex-col gap-0.5">
                    <p className={`text-[12px] ${passwordChecks.length ? "text-green-600" : "text-[var(--color-text-sub)]"}`}>
                      {passwordChecks.length ? "✓" : "○"} 8자 이상
                    </p>
                    <p className={`text-[12px] ${satisfiedTypes >= 3 ? "text-green-600" : "text-[var(--color-text-sub)]"}`}>
                      {satisfiedTypes >= 3 ? "✓" : "○"} 대문자/소문자/숫자/특수문자 중 3종 이상 포함
                    </p>
                  </div>
                )}
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-semibold text-[var(--color-text-title)]">비밀번호 확인</label>
                <div className="relative">
                  <input type={showPasswordConfirm ? "text" : "password"} placeholder="비밀번호를 다시 입력하세요" value={passwordConfirm} onChange={(e) => setPasswordConfirm(e.target.value)} className={inputCls} autoComplete="new-password" />
                  <button type="button" onClick={() => setShowPasswordConfirm(!showPasswordConfirm)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-sub)] hover:text-[var(--color-text-body)]">
                    {showPasswordConfirm ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {passwordConfirm.length > 0 && (
                  <p className={`text-[12px] mt-0.5 ${passwordMatch ? "text-green-600" : "text-[var(--color-error)]"}`}>
                    {passwordMatch ? "✓ 비밀번호가 일치합니다" : "✗ 비밀번호가 일치하지 않습니다"}
                  </p>
                )}
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

              <label className="flex items-start gap-2 cursor-pointer">
                <input type="checkbox" checked={agreeTerms} onChange={(e) => setAgreeTerms(e.target.checked)} className="mt-1 accent-[var(--color-pilates)]" />
                <span className="text-[13px] text-[var(--color-text-body)]">
                  [필수] 이용약관 및 개인정보 수집·이용에 동의합니다
                </span>
              </label>

              <button
                onClick={handleSignup}
                disabled={loading || !name || !passwordValid || !passwordMatch || !agreeTerms}
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
              <input type="tel" placeholder="010-1234-5678" value={phone} onChange={(e) => setPhone(formatPhone(e.target.value))} className={inputCls} />
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
