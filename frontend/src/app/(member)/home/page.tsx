"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useAuthStore } from "@/lib/store/auth-store";
import { memberApi } from "@/lib/api/member";
import type { Member, Membership, Reservation } from "@/lib/types/domain";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Calendar, Ticket, Clock } from "lucide-react";
import { formatTime } from "@/lib/utils/format";

export default function MemberHomePage() {
  const { user } = useAuthStore();
  const [member, setMember] = useState<Member | null>(null);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [me, ms, rs] = await Promise.all([
          memberApi.getMe(),
          memberApi.getMemberships(),
          memberApi.getReservations(),
        ]);
        setMember(me);
        setMemberships(ms);
        setReservations(rs);
      } catch {
        // 로딩 실패 시 빈 상태
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const activeMembership = memberships.find((m) => m.status === "ACTIVE");
  const upcomingReservations = reservations
    .filter((r) => r.status === "CONFIRMED")
    .slice(0, 3);

  if (loading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="animate-pulse text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto p-4 space-y-4">
      {/* 환영 */}
      <div>
        <h1 className="text-2xl font-bold">
          안녕하세요, {member?.name || "회원"}님
        </h1>
        <p className="text-muted-foreground text-sm">
          오늘도 건강한 하루 보내세요
        </p>
      </div>

      {/* 정기권 카드 */}
      <Card>
        <CardHeader className="pb-2">
          <div className="flex items-center gap-2">
            <Ticket className="h-4 w-4" />
            <CardTitle className="text-base">내 정기권</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          {activeMembership ? (
            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <span className="font-medium">
                  {activeMembership.passName}
                </span>
                <Badge variant="secondary">
                  {activeMembership.unlimited
                    ? "무제한"
                    : `${activeMembership.remainingCount}/${activeMembership.totalCount}회`}
                </Badge>
              </div>
              <p className="text-sm text-muted-foreground">
                {activeMembership.startDate} ~ {activeMembership.endDate}
              </p>
              {!activeMembership.unlimited && (
                <div className="w-full bg-muted rounded-full h-2">
                  <div
                    className="bg-primary h-2 rounded-full transition-all"
                    style={{
                      width: `${
                        (activeMembership.remainingCount /
                          activeMembership.totalCount) *
                        100
                      }%`,
                    }}
                  />
                </div>
              )}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              활성 정기권이 없습니다.
            </p>
          )}
        </CardContent>
      </Card>

      {/* 다가오는 예약 */}
      <Card>
        <CardHeader className="pb-2">
          <div className="flex items-center gap-2">
            <Clock className="h-4 w-4" />
            <CardTitle className="text-base">다가오는 예약</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          {upcomingReservations.length > 0 ? (
            <div className="space-y-3">
              {upcomingReservations.map((r) => (
                <div
                  key={r.id}
                  className="flex justify-between items-center py-2 border-b last:border-0"
                >
                  <div>
                    <p className="font-medium text-sm">{r.lessonTypeName}</p>
                    <p className="text-xs text-muted-foreground">
                      {r.classDate} {formatTime(r.startTime)}~
                      {formatTime(r.endTime)} · {r.instructorName}
                    </p>
                  </div>
                  <Badge>{r.status === "CONFIRMED" ? "확정" : r.status}</Badge>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              예정된 예약이 없습니다.
            </p>
          )}
        </CardContent>
      </Card>

      {/* 빠른 액션 */}
      <div className="grid grid-cols-2 gap-3">
        <Link href="/schedule">
          <Button variant="outline" className="w-full h-14">
            <Calendar className="h-4 w-4 mr-2" />
            시간표 보기
          </Button>
        </Link>
        <Link href="/schedule">
          <Button className="w-full h-14">
            <Calendar className="h-4 w-4 mr-2" />
            예약하기
          </Button>
        </Link>
      </div>
    </div>
  );
}
