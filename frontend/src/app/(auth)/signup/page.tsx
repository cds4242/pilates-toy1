"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useAuthStore } from "@/lib/store/auth-store";
import { authApi } from "@/lib/api/auth";

type Step = "phone" | "verify" | "info" | "done";

export default function SignupPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  const [step, setStep] = useState<Step>("phone");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [verifiedToken, setVerifiedToken] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [gender, setGender] = useState("FEMALE");
  const [loading, setLoading] = useState(false);

  const handleRequestSms = async () => {
    setLoading(true);
    try {
      await authApi.requestSms(phone.replace(/-/g, ""));
      toast.success("인증번호가 발송되었습니다.");
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
    setLoading(true);
    try {
      const res = await authApi.signup({
        verifiedToken,
        name,
        password,
        gender,
      });
      const payload = JSON.parse(atob(res.accessToken.split(".")[1]));
      login(res.accessToken, res.refreshToken, {
        id: payload.sub,
        role: "MEMBER",
        name,
      });
      toast.success("가입이 완료되었습니다!");
      setStep("done");
      router.push("/home");
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "가입 실패");
    } finally {
      setLoading(false);
    }
  };

  const stepLabels = ["휴대폰 인증", "인증번호 확인", "회원 정보", "완료"];
  const stepIndex = { phone: 0, verify: 1, info: 2, done: 3 }[step];

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle className="text-2xl">회원가입</CardTitle>
        <CardDescription>
          {stepLabels[stepIndex]} ({stepIndex + 1}/{stepLabels.length})
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Progress bar */}
        <div className="flex gap-1">
          {stepLabels.map((_, i) => (
            <div
              key={i}
              className={`h-1 flex-1 rounded-full ${
                i <= stepIndex ? "bg-primary" : "bg-muted"
              }`}
            />
          ))}
        </div>

        {step === "phone" && (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>휴대폰 번호</Label>
              <Input
                placeholder="01012345678"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </div>
            <Button
              className="w-full"
              onClick={handleRequestSms}
              disabled={loading || phone.length < 10}
            >
              {loading ? "발송 중..." : "인증번호 받기"}
            </Button>
          </div>
        )}

        {step === "verify" && (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>인증번호 6자리</Label>
              <Input
                placeholder="123456"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                maxLength={6}
              />
            </div>
            <Button
              className="w-full"
              onClick={handleVerify}
              disabled={loading || code.length < 6}
            >
              {loading ? "확인 중..." : "인증 확인"}
            </Button>
          </div>
        )}

        {step === "info" && (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>이름</Label>
              <Input
                placeholder="홍길동"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>비밀번호</Label>
              <Input
                type="password"
                placeholder="8자 이상, 대소문자/숫자/특수문자 중 3종"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>성별</Label>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant={gender === "FEMALE" ? "default" : "outline"}
                  className="flex-1"
                  onClick={() => setGender("FEMALE")}
                >
                  여성
                </Button>
                <Button
                  type="button"
                  variant={gender === "MALE" ? "default" : "outline"}
                  className="flex-1"
                  onClick={() => setGender("MALE")}
                >
                  남성
                </Button>
              </div>
            </div>
            <Button
              className="w-full"
              onClick={handleSignup}
              disabled={loading || !name || password.length < 8}
            >
              {loading ? "가입 중..." : "가입 완료"}
            </Button>
          </div>
        )}

        {step !== "done" && (
          <p className="text-center text-sm text-muted-foreground">
            이미 회원이신가요?{" "}
            <Link href="/login" className="underline font-medium">
              로그인
            </Link>
          </p>
        )}
      </CardContent>
    </Card>
  );
}
