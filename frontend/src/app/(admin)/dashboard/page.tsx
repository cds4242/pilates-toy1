"use client";

import { useEffect, useState } from "react";
import { adminApi } from "@/lib/api/admin";
import type { DashboardData } from "@/lib/types/domain";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Calendar,
  DollarSign,
  AlertTriangle,
  Clock,
} from "lucide-react";
import { formatTime } from "@/lib/utils/format";

export default function AdminDashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const d = await adminApi.getDashboard();
        setData(d);
      } catch {
        // empty
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">대시보드</h1>
        <div className="flex items-center justify-center p-8">
          <div className="animate-pulse text-muted-foreground">로딩 중...</div>
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">대시보드</h1>
        <div className="text-center text-muted-foreground p-8">
          대시보드 데이터를 불러올 수 없습니다.
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">대시보드</h1>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {/* 오늘 수업 요약 */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">오늘 수업</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {data.todayClasses.count}건
            </div>
          </CardContent>
        </Card>

        {/* 이번 주 매출 */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">이번 주 매출</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {Number(data.thisWeekRevenue.total).toLocaleString()}원
            </div>
          </CardContent>
        </Card>

        {/* 만료 임박 */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">만료 임박</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {data.expiringMemberships.length}명
            </div>
          </CardContent>
        </Card>

        {/* 알림 */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">주의 알림</CardTitle>
            <AlertTriangle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {data.alerts.noShowMembers.length +
                data.alerts.lowMembershipMembers.length}
              건
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {/* 오늘 수업 상세 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">오늘의 수업</CardTitle>
          </CardHeader>
          <CardContent>
            {data.todayClasses.schedules.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                오늘 예정된 수업이 없습니다.
              </p>
            ) : (
              <div className="space-y-3">
                {data.todayClasses.schedules.map((s, i) => (
                  <div
                    key={i}
                    className="flex justify-between items-center py-2 border-b last:border-0"
                  >
                    <div>
                      <p className="font-medium text-sm">
                        {formatTime(s.time)} · {s.className}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {s.instructor}
                      </p>
                    </div>
                    <Badge variant="secondary">
                      {s.reservedCount}/{s.capacity}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* 만료 임박 정기권 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">만료 임박 정기권</CardTitle>
          </CardHeader>
          <CardContent>
            {data.expiringMemberships.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                7일 이내 만료 정기권이 없습니다.
              </p>
            ) : (
              <div className="space-y-3">
                {data.expiringMemberships.map((m) => (
                  <div
                    key={m.memberId}
                    className="flex justify-between items-center py-2 border-b last:border-0"
                  >
                    <div>
                      <p className="font-medium text-sm">{m.memberName}</p>
                      <p className="text-xs text-muted-foreground">
                        {m.passName}
                      </p>
                    </div>
                    <Badge
                      variant={m.daysLeft <= 2 ? "destructive" : "secondary"}
                    >
                      D-{m.daysLeft}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* 노쇼 알림 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">노쇼 주의 회원</CardTitle>
          </CardHeader>
          <CardContent>
            {data.alerts.noShowMembers.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                노쇼 주의 회원이 없습니다.
              </p>
            ) : (
              <div className="space-y-2">
                {data.alerts.noShowMembers.map((m) => (
                  <div
                    key={m.memberId}
                    className="flex justify-between items-center"
                  >
                    <span className="text-sm">{m.memberName}</span>
                    <Badge variant="destructive">{m.noShowCount}회</Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* 잔여 부족 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">정기권 잔여 부족</CardTitle>
          </CardHeader>
          <CardContent>
            {data.alerts.lowMembershipMembers.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                잔여 부족 회원이 없습니다.
              </p>
            ) : (
              <div className="space-y-2">
                {data.alerts.lowMembershipMembers.map((m) => (
                  <div
                    key={m.memberId}
                    className="flex justify-between items-center"
                  >
                    <div>
                      <span className="text-sm">{m.memberName}</span>
                      <span className="text-xs text-muted-foreground ml-2">
                        {m.passName}
                      </span>
                    </div>
                    <Badge variant="secondary">{m.remainingCount}회 남음</Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
