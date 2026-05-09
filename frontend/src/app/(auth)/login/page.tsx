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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuthStore } from "@/lib/store/auth-store";
import { authApi } from "@/lib/api/auth";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();

  // 회원 로그인
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [memberLoading, setMemberLoading] = useState(false);

  // 관리자 로그인
  const [loginId, setLoginId] = useState("");
  const [adminPw, setAdminPw] = useState("");
  const [adminLoading, setAdminLoading] = useState(false);

  const handleMemberLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setMemberLoading(true);
    try {
      const res = await authApi.login(phone.replace(/-/g, ""), password);
      // JWT에서 sub(memberId), role 추출
      const payload = JSON.parse(atob(res.accessToken.split(".")[1]));
      login(res.accessToken, res.refreshToken, {
        id: payload.sub,
        role: payload.role || "MEMBER",
        name: "회원",
      });
      toast.success("로그인 성공");
      router.push("/home");
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "로그인에 실패했습니다.";
      toast.error(msg);
    } finally {
      setMemberLoading(false);
    }
  };

  const handleAdminLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setAdminLoading(true);
    try {
      const res = await authApi.adminLogin(loginId, adminPw);
      login(res.accessToken, res.refreshToken, {
        id: String(res.adminId),
        role: res.role,
        name: "관리자",
      });
      toast.success("로그인 성공");
      if (res.role === "INSTRUCTOR") {
        router.push("/instructor/schedule");
      } else {
        router.push("/dashboard");
      }
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "로그인에 실패했습니다.";
      toast.error(msg);
    } finally {
      setAdminLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle className="text-2xl">Pilates Studio</CardTitle>
        <CardDescription>로그인하여 시작하세요</CardDescription>
      </CardHeader>
      <CardContent>
        <Tabs defaultValue="member">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="member">회원</TabsTrigger>
            <TabsTrigger value="admin">관리자</TabsTrigger>
          </TabsList>

          <TabsContent value="member">
            <form onSubmit={handleMemberLogin} className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label htmlFor="phone">휴대폰 번호</Label>
                <Input
                  id="phone"
                  placeholder="01012345678"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">비밀번호</Label>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
              <Button type="submit" className="w-full" disabled={memberLoading}>
                {memberLoading ? "로그인 중..." : "로그인"}
              </Button>
              <p className="text-center text-sm text-muted-foreground">
                아직 회원이 아니신가요?{" "}
                <Link href="/signup" className="underline font-medium">
                  회원가입
                </Link>
              </p>
            </form>
          </TabsContent>

          <TabsContent value="admin">
            <form onSubmit={handleAdminLogin} className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label htmlFor="loginId">아이디</Label>
                <Input
                  id="loginId"
                  placeholder="관리자 아이디"
                  value={loginId}
                  onChange={(e) => setLoginId(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="adminPw">비밀번호</Label>
                <Input
                  id="adminPw"
                  type="password"
                  value={adminPw}
                  onChange={(e) => setAdminPw(e.target.value)}
                  required
                />
              </div>
              <Button type="submit" className="w-full" disabled={adminLoading}>
                {adminLoading ? "로그인 중..." : "관리자 로그인"}
              </Button>
            </form>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
