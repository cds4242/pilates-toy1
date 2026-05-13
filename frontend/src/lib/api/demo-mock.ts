"use client";

/**
 * 시연용 mock API 응답기.
 * NEXT_PUBLIC_DEMO_MODE=true이고 백엔드가 없을 때, axios interceptor에서
 * 모든 /api/* 요청을 가로채 박제 시점의 풍부한 더미 데이터를 반환한다.
 *
 * 동작 원리:
 *   axios request interceptor에서 adapter를 바꿔 네트워크 호출 자체를 차단하고,
 *   본 모듈의 mockMatch()로 응답을 만들어 즉시 resolve한다.
 */

import type {
  InternalAxiosRequestConfig,
  AxiosResponse,
  AxiosAdapter,
} from "axios";

interface ApiEnvelope<T> {
  success: true;
  data: T;
  error: null;
}

function ok<T>(data: T): ApiEnvelope<T> {
  return { success: true, data, error: null };
}

// ── 날짜 헬퍼 (박제 시점 5/11 기준이지만 "오늘 = 박제 D-Day" 느낌 유지) ──
function pad(n: number) {
  return n.toString().padStart(2, "0");
}
function ymd(d: Date) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
const today = new Date();
const tomorrow = new Date(today.getTime() + 86400000);
const dayAfter = new Date(today.getTime() + 2 * 86400000);
const TODAY = ymd(today);
const TOMORROW = ymd(tomorrow);
const DAY_AFTER = ymd(dayAfter);

// ── 회원 (김데모) ──
const MEMBER_ME = {
  publicId: "MBR-DEMO-0001",
  name: "김데모",
  phoneNumber: "010-0000-0001",
  gender: "FEMALE",
  birthDate: "1995-03-12",
  status: "ACTIVE",
  profileImageUrl: null,
  createdAt: "2026-05-10T09:00:00",
};

const MEMBER_MEMBERSHIPS = [
  {
    publicId: "MBSH-0001",
    passName: "12회권",
    totalCount: 12,
    remainingCount: 9,
    unlimited: false,
    startDate: "2026-04-10",
    endDate: "2026-07-09",
    status: "ACTIVE",
  },
];

function daysAgo(n: number) {
  return ymd(new Date(today.getTime() - n * 86400000));
}
const MEMBER_RESERVATIONS = [
  // 다가오는 예약
  { id: 101, classScheduleId: 1001, classDate: TOMORROW, startTime: "11:00", endTime: "11:50", instructorName: "박지영", lessonTypeName: "듀엣", status: "CONFIRMED" },
  { id: 102, classScheduleId: 1002, classDate: TOMORROW, startTime: "14:00", endTime: "14:50", instructorName: "이수진", lessonTypeName: "개인", status: "CONFIRMED" },
  { id: 103, classScheduleId: 1003, classDate: TOMORROW, startTime: "17:00", endTime: "17:50", instructorName: "최재훈", lessonTypeName: "그룹", status: "CONFIRMED" },
  { id: 104, classScheduleId: 1004, classDate: DAY_AFTER, startTime: "07:00", endTime: "07:50", instructorName: "이수진", lessonTypeName: "그룹", status: "CONFIRMED" },
  { id: 105, classScheduleId: 1005, classDate: DAY_AFTER, startTime: "10:00", endTime: "10:50", instructorName: "김하늘", lessonTypeName: "개인", status: "CONFIRMED" },
  // 지난 출석
  { id: 201, classScheduleId: 2001, classDate: daysAgo(2), startTime: "10:00", endTime: "10:50", instructorName: "박지영", lessonTypeName: "그룹", status: "ATTENDED" },
  { id: 202, classScheduleId: 2002, classDate: daysAgo(4), startTime: "11:00", endTime: "11:50", instructorName: "이수진", lessonTypeName: "듀엣", status: "ATTENDED" },
  { id: 203, classScheduleId: 2003, classDate: daysAgo(6), startTime: "14:00", endTime: "14:50", instructorName: "최재훈", lessonTypeName: "그룹", status: "ATTENDED" },
  { id: 204, classScheduleId: 2004, classDate: daysAgo(9), startTime: "19:00", endTime: "19:50", instructorName: "박지영", lessonTypeName: "그룹", status: "ATTENDED" },
  // 지난 노쇼
  { id: 301, classScheduleId: 3001, classDate: daysAgo(1), startTime: "07:00", endTime: "07:50", instructorName: "류시현", lessonTypeName: "그룹", status: "NO_SHOW" },
  { id: 302, classScheduleId: 3002, classDate: daysAgo(3), startTime: "10:00", endTime: "10:50", instructorName: "이수진", lessonTypeName: "그룹", status: "NO_SHOW" },
  { id: 303, classScheduleId: 3003, classDate: daysAgo(5), startTime: "09:00", endTime: "09:50", instructorName: "박지영", lessonTypeName: "그룹", status: "NO_SHOW" },
  // 지난 취소
  { id: 401, classScheduleId: 4001, classDate: daysAgo(7), startTime: "09:00", endTime: "09:50", instructorName: "최재훈", lessonTypeName: "듀엣", status: "CANCELED" },
  { id: 402, classScheduleId: 4002, classDate: daysAgo(10), startTime: "15:00", endTime: "15:50", instructorName: "한가람", lessonTypeName: "듀엣", status: "CANCELED" },
];

const MEMBER_ATTENDANCES = [
  { id: 1, classDate: daysAgo(2), startTime: "10:00", endTime: "10:50", lessonTypeName: "그룹", instructorName: "박지영", status: "ATTENDED" },
  { id: 2, classDate: daysAgo(4), startTime: "11:00", endTime: "11:50", lessonTypeName: "듀엣", instructorName: "이수진", status: "ATTENDED" },
  { id: 3, classDate: daysAgo(6), startTime: "14:00", endTime: "14:50", lessonTypeName: "그룹", instructorName: "최재훈", status: "ATTENDED" },
  { id: 4, classDate: daysAgo(9), startTime: "19:00", endTime: "19:50", lessonTypeName: "그룹", instructorName: "박지영", status: "ATTENDED" },
  { id: 5, classDate: daysAgo(1), startTime: "07:00", endTime: "07:50", lessonTypeName: "그룹", instructorName: "류시현", status: "NO_SHOW" },
  { id: 6, classDate: daysAgo(3), startTime: "10:00", endTime: "10:50", lessonTypeName: "그룹", instructorName: "이수진", status: "NO_SHOW" },
  { id: 7, classDate: daysAgo(7), startTime: "09:00", endTime: "09:50", lessonTypeName: "듀엣", instructorName: "최재훈", status: "CANCELED" },
];

// ── 강사 ──
const INSTRUCTORS = [
  { id: 1, publicId: "INS-01", name: "박지영", phone: "010-1111-2222", specialty: "기구 필라테스, 매트 필라테스", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "평일 10:00-20:00" },
  { id: 2, publicId: "INS-02", name: "이수진", phone: "010-3333-4444", specialty: "재활 필라테스, 산전산후", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "평일 14:00-21:00" },
  { id: 3, publicId: "INS-03", name: "최재훈", phone: "010-5555-6666", specialty: "기구 필라테스, 체형교정", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "평일 07:00-15:00" },
  { id: 4, publicId: "INS-04", name: "김하늘", phone: "010-1003-1003", specialty: "개인 레슨 전문", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "평일 09:00-18:00" },
  { id: 5, publicId: "INS-05", name: "정유진", phone: "010-1004-1004", specialty: "그룹 수업", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "주말 위주" },
  { id: 6, publicId: "INS-06", name: "한가람", phone: "010-1005-1005", specialty: "듀엣 전문", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "평일 13:00-20:00" },
  { id: 7, publicId: "INS-07", name: "오선아", phone: "010-1006-1006", specialty: "재활, 자세 교정", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "평일 16:00-21:00" },
  { id: 8, publicId: "INS-08", name: "윤지민", phone: "010-1007-1007", specialty: "체형 교정", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "평일 07:00-12:00" },
  { id: 9, publicId: "INS-09", name: "류시현", phone: "010-1008-1008", specialty: "초보자 레슨", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "평일 07:00-13:00" },
  { id: 10, publicId: "INS-10", name: "박데모", phone: "010-0000-0002", specialty: "그룹/듀엣", status: "ACTIVE", active: true, createdAt: "2026-05-10", workingHoursMemo: "전일 가능" },
];

// ── 수강권 상품 ──
const MEMBERSHIP_PASSES = [
  { id: 1, publicId: "PASS-01", name: "8회권", category: "GROUP", price: 180000, totalCount: 8, validityDays: 60, validDays: 60, unlimited: false, monthlyLimit: null, visible: true, active: true, displayOrder: 1, sortOrder: 1, description: "주 2회 추천, 그룹/듀엣 수업 가능", saleStartDate: null, saleEndDate: null },
  { id: 2, publicId: "PASS-02", name: "12회권", category: "GROUP", price: 250000, totalCount: 12, validityDays: 90, validDays: 90, unlimited: false, monthlyLimit: null, visible: true, active: true, displayOrder: 2, sortOrder: 2, description: "주 3회 추천, 그룹/듀엣 수업 가능", saleStartDate: null, saleEndDate: null, best: true },
  { id: 3, publicId: "PASS-03", name: "무제한권", category: "UNLIMITED", price: 350000, totalCount: null, validityDays: 30, validDays: 30, unlimited: true, monthlyLimit: 30, visible: true, active: true, displayOrder: 3, sortOrder: 3, description: "한 달 무제한 수강, 월 30회까지", saleStartDate: null, saleEndDate: null },
  { id: 4, publicId: "PASS-04", name: "개인 10회권", category: "PERSONAL", price: 500000, totalCount: 10, validityDays: 90, validDays: 90, unlimited: false, monthlyLimit: null, visible: true, active: true, displayOrder: 4, sortOrder: 4, description: "1:1 개인 레슨 전용", saleStartDate: null, saleEndDate: null },
];

// ── 수업 유형 ──
const LESSON_TYPES = [
  { id: 1, name: "개인", maxCapacity: 1, defaultDurationMinutes: 50, active: true },
  { id: 2, name: "듀엣", maxCapacity: 2, defaultDurationMinutes: 50, active: true },
  { id: 3, name: "그룹", maxCapacity: 8, defaultDurationMinutes: 50, active: true },
  { id: 4, name: "체험", maxCapacity: 1, defaultDurationMinutes: 50, active: true },
];

// ── 관리자 회원 상세 (id별 mock) ──
function makeMemberDetail(memberId: number) {
  // 박제본 데이터를 기반으로 — 1번은 김데모(12회권), 6번 홍성민(12회권), 7번 전현우(무제한)
  const tpl = ADMIN_MEMBER_NAMES[(memberId - 1) % ADMIN_MEMBER_NAMES.length];
  const name = memberId <= ADMIN_MEMBER_NAMES.length ? tpl[0] : `회원${memberId}`;
  const phone = memberId <= ADMIN_MEMBER_NAMES.length ? tpl[1] : `010-9${pad(memberId)}-${pad(memberId % 100)}${pad((memberId + 7) % 100)}`;
  const passName = tpl[2];
  const rateStr = tpl[5];
  const rate = rateStr === "-" ? 0 : parseInt(String(rateStr).replace("%", ""), 10) || 0;
  const memberships = passName
    ? [{
        id: memberId * 10,
        passName,
        status: tpl[4] === "만료됨" ? "EXPIRED" : "ACTIVE",
        totalCount: passName === "무제한권" ? 0 : passName === "8회권" ? 8 : passName === "12회권" ? 12 : 10,
        remainingCount: passName === "무제한권" ? 0 : parseInt(String(tpl[3]).split("/")[0], 10) || 0,
        unlimited: passName === "무제한권",
        startDate: "2026-04-10",
        endDate: tpl[4] === "-" || tpl[4] === "만료됨" ? null : `2026-${tpl[4]}`,
      }]
    : [];
  const noShowCount = memberId === 11 ? 7 : memberId === 12 ? 6 : memberId === 1 ? 5 : memberId === 13 ? 5 : 0;
  return {
    id: memberId,
    publicId: `MBR-${pad(memberId)}`,
    name,
    phone,
    birthDate: memberId === 1 ? "1995-03-12" : null,
    gender: memberId % 3 === 0 ? "MALE" : "FEMALE",
    status: "ACTIVE",
    profileImageUrl: null,
    createdAt: "2026-05-10",
    memberships,
    attendanceRate: { overallRate: rate, recent90DayRate: rate },
    noShowCount,
    memos: memberId === 1
      ? [
          { id: 1, content: "회사원 · 평일 저녁/주말 위주로 예약", writerName: "데모관리자", createdAt: "2026-05-10T10:00:00", updatedAt: "2026-05-10T10:00:00" },
          { id: 2, content: "허리 통증 호소, 가벼운 운동 위주로 추천", writerName: "데모관리자", createdAt: "2026-05-08T14:30:00", updatedAt: "2026-05-08T14:30:00" },
        ]
      : [],
  };
}

// ── 회원 목록 (관리자용) ──
const ADMIN_MEMBER_NAMES = [
  ["김데모", "010-0000-0001", "12회권", "9/12", "07/09", "0%"],
  ["서한결", "010-9076-1076", null, "-", "-", "-"],
  ["안주원", "010-9077-1077", null, "-", "-", "-"],
  ["남예준", "010-9078-1078", null, "-", "-", "-"],
  ["황태민", "010-9079-1079", null, "-", "-", "-"],
  ["홍성민", "010-9073-1073", "12회권", "10/12", "07/20", "88%"],
  ["전현우", "010-9074-1074", "무제한권", "무제한", "05/13", "100%"],
  ["류지훈", "010-9075-1075", null, "-", "-", "-"],
  ["유서진", "010-9069-1069", "12회권", "11/12", "06/30", "50%"],
  ["신연우", "010-9070-1070", "무제한권", "무제한", "만료됨", "75%"],
  ["변하림", "010-9071-1071", "12회권", "5/12", "07/14", "30%"],
  ["정하윤", "010-9072-1072", "8회권", "3/8", "06/22", "60%"],
  ["박지호", "010-9080-1080", "8회권", "5/8", "07/01", "70%"],
  ["이서연", "010-9081-1081", "12회권", "2/12", "07/15", "12%"],
  ["조하은", "010-9082-1082", null, "-", "-", "-"],
  ["송민재", "010-9083-1083", "개인 10회권", "8/10", "08/02", "92%"],
];
const ADMIN_MEMBERS_TOTAL = 76;
function makeAdminMembers(page = 0, size = 10) {
  // 박제본은 10명씩 8페이지 (76명). page 0만 정확히 재현하고 다른 페이지는 이름만 다르게 더미.
  const content = [];
  const start = page * size;
  for (let i = 0; i < size; i++) {
    const idx = start + i;
    if (idx >= ADMIN_MEMBERS_TOTAL) break;
    const tpl = ADMIN_MEMBER_NAMES[idx % ADMIN_MEMBER_NAMES.length];
    const name = idx < ADMIN_MEMBER_NAMES.length ? tpl[0] : `회원${idx + 1}`;
    const phone = idx < ADMIN_MEMBER_NAMES.length ? tpl[1] : `010-9${pad(idx)}-${pad(idx % 100)}${pad((idx + 7) % 100)}`;
    const passName = tpl[2];
    const remainingDisplay = tpl[3];
    const endDate = tpl[4] === "-" || tpl[4] === "만료됨" ? null : `2026-${tpl[4]}`;
    const attendanceRate = tpl[5] === "-" ? 0 : parseInt(String(tpl[5]).replace("%", ""), 10);
    const expired = tpl[4] === "만료됨";
    content.push({
      id: idx + 1,
      publicId: `MBR-${pad(idx + 1)}`,
      name,
      phone,
      phoneNumber: phone,
      gender: idx % 3 === 0 ? "MALE" : "FEMALE",
      activeMembership: passName || "-",
      passName,
      membershipName: passName,
      remainingInfo: remainingDisplay,
      remainingDisplay,
      expiryDate: endDate,
      endDate,
      expired,
      attendanceRate: tpl[5] === "-" ? "-" : String(tpl[5]),
      status: "ACTIVE",
      createdAt: "2026-05-10",
      profileImageUrl: null,
    });
  }
  return {
    content,
    totalElements: ADMIN_MEMBERS_TOTAL,
    totalPages: Math.ceil(ADMIN_MEMBERS_TOTAL / size),
    number: page,
    size,
    first: page === 0,
    last: page >= Math.ceil(ADMIN_MEMBERS_TOTAL / size) - 1,
    empty: content.length === 0,
  };
}

// ── 대시보드 ──
const DASHBOARD = {
  todayClasses: {
    count: 6,
    schedules: [
      { time: "07:00", instructor: "류시현", className: "그룹", reservedCount: 4, capacity: 8 },
      { time: "10:00", instructor: "박데모", className: "그룹", reservedCount: 0, capacity: 8 },
      { time: "14:00", instructor: "박데모", className: "그룹", reservedCount: 0, capacity: 8 },
      { time: "15:00", instructor: "한가람", className: "듀엣", reservedCount: 0, capacity: 2 },
      { time: "17:00", instructor: "오선아", className: "듀엣", reservedCount: 0, capacity: 2 },
      { time: "19:00", instructor: "이수진", className: "그룹", reservedCount: 8, capacity: 8 },
    ],
  },
  todayReservations: 12,
  thisWeekRevenue: {
    total: 2500000,
    breakdown: [
      { date: ymd(new Date(today.getTime() - 6 * 86400000)), amount: 0 },
      { date: ymd(new Date(today.getTime() - 5 * 86400000)), amount: 350000 },
      { date: ymd(new Date(today.getTime() - 4 * 86400000)), amount: 180000 },
      { date: ymd(new Date(today.getTime() - 3 * 86400000)), amount: 500000 },
      { date: ymd(new Date(today.getTime() - 2 * 86400000)), amount: 250000 },
      { date: ymd(new Date(today.getTime() - 1 * 86400000)), amount: 420000 },
      { date: TODAY, amount: 800000 },
    ],
  },
  expiringMemberships: [
    { memberId: 7, memberName: "전현우", passName: "무제한권", daysLeft: 2, endDate: "2026-05-13" },
    { memberId: 9, memberName: "유서진", passName: "12회권", daysLeft: 50, endDate: "2026-06-30" },
    { memberId: 1, memberName: "김데모", passName: "12회권", daysLeft: 59, endDate: "2026-07-09" },
    { memberId: 6, memberName: "홍성민", passName: "12회권", daysLeft: 70, endDate: "2026-07-20" },
  ],
  expiringCount: 12,
  alerts: {
    noShowMembers: [
      { memberId: 11, memberName: "변하림", noShowCount: 7 },
      { memberId: 12, memberName: "유서진", noShowCount: 6 },
      { memberId: 1, memberName: "김데모", noShowCount: 5 },
      { memberId: 13, memberName: "정하윤", noShowCount: 5 },
    ],
    lowMembershipMembers: [
      { memberId: 14, memberName: "이서연", passName: "12회권", remainingCount: 2 },
      { memberId: 12, memberName: "정하윤", passName: "8회권", remainingCount: 3 },
    ],
  },
};

function makeRevenueForPeriod(period: string) {
  if (period === "month") {
    // 최근 30일 일자별 매출
    const days = 30;
    const breakdown = [] as Array<{ date: string; amount: number }>;
    let total = 0;
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(today.getTime() - i * 86400000);
      // 주중에 더 많은 매출
      const dow = d.getDay();
      const base = dow === 0 || dow === 6 ? 0 : 80000 + (i % 3) * 70000;
      const amount = i % 7 === 0 ? base + 250000 : base;
      total += amount;
      breakdown.push({ date: ymd(d), amount });
    }
    return { total, breakdown };
  }
  // 주간: 최근 7일
  const breakdown = [] as Array<{ date: string; amount: number }>;
  let total = 0;
  const weekAmounts = [0, 350000, 180000, 500000, 250000, 420000, 800000];
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today.getTime() - i * 86400000);
    const amount = weekAmounts[(6 - i + d.getDay()) % 7];
    total += amount;
    breakdown.push({ date: ymd(d), amount });
  }
  return { total, breakdown };
}

// ── 시간표 / 수업 ──
type ScheduleRow = { time: string; lessonTypeName: string; instructorName: string; reserved: number; capacity: number };
const WEEKLY_SCHEDULE: Record<string, ScheduleRow[]> = {
  Mon: [
    { time: "07:00", lessonTypeName: "그룹", instructorName: "류시현", reserved: 4, capacity: 8 },
    { time: "09:00", lessonTypeName: "그룹", instructorName: "류시현", reserved: 4, capacity: 8 },
    { time: "10:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
    { time: "11:00", lessonTypeName: "듀엣", instructorName: "박지영", reserved: 2, capacity: 2 },
    { time: "14:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
    { time: "15:00", lessonTypeName: "듀엣", instructorName: "한가람", reserved: 0, capacity: 2 },
    { time: "17:00", lessonTypeName: "듀엣", instructorName: "오선아", reserved: 0, capacity: 2 },
    { time: "18:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
    { time: "19:00", lessonTypeName: "그룹", instructorName: "이수진", reserved: 8, capacity: 8 },
  ],
  Tue: [
    { time: "09:00", lessonTypeName: "그룹", instructorName: "박지영", reserved: 4, capacity: 8 },
    { time: "10:00", lessonTypeName: "개인", instructorName: "김하늘", reserved: 1, capacity: 1 },
    { time: "11:00", lessonTypeName: "듀엣", instructorName: "박지영", reserved: 2, capacity: 2 },
    { time: "14:00", lessonTypeName: "개인", instructorName: "이수진", reserved: 1, capacity: 1 },
    { time: "14:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
    { time: "15:00", lessonTypeName: "개인", instructorName: "이수진", reserved: 1, capacity: 1 },
    { time: "17:00", lessonTypeName: "그룹", instructorName: "최재훈", reserved: 7, capacity: 8 },
    { time: "18:00", lessonTypeName: "듀엣", instructorName: "한가람", reserved: 2, capacity: 2 },
    { time: "18:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
    { time: "19:00", lessonTypeName: "체험", instructorName: "박지영", reserved: 1, capacity: 1 },
  ],
  Wed: [
    { time: "07:00", lessonTypeName: "그룹", instructorName: "이수진", reserved: 7, capacity: 8 },
    { time: "09:00", lessonTypeName: "그룹", instructorName: "류시현", reserved: 4, capacity: 8 },
    { time: "10:00", lessonTypeName: "듀엣", instructorName: "류시현", reserved: 0, capacity: 2 },
    { time: "10:00", lessonTypeName: "개인", instructorName: "김하늘", reserved: 1, capacity: 1 },
    { time: "11:00", lessonTypeName: "그룹", instructorName: "류시현", reserved: 6, capacity: 8 },
    { time: "12:00", lessonTypeName: "듀엣", instructorName: "이수진", reserved: 0, capacity: 2 },
    { time: "15:00", lessonTypeName: "듀엣", instructorName: "박지영", reserved: 0, capacity: 2 },
    { time: "18:00", lessonTypeName: "듀엣", instructorName: "한가람", reserved: 2, capacity: 2 },
    { time: "18:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
    { time: "19:00", lessonTypeName: "그룹", instructorName: "박지영", reserved: 8, capacity: 8 },
    { time: "19:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
  ],
  Thu: [
    { time: "07:00", lessonTypeName: "개인", instructorName: "윤지민", reserved: 1, capacity: 1 },
    { time: "10:00", lessonTypeName: "듀엣", instructorName: "이수진", reserved: 0, capacity: 2 },
    { time: "11:00", lessonTypeName: "그룹", instructorName: "류시현", reserved: 6, capacity: 8 },
    { time: "12:00", lessonTypeName: "그룹", instructorName: "박지영", reserved: 5, capacity: 8 },
    { time: "14:00", lessonTypeName: "그룹", instructorName: "오선아", reserved: 3, capacity: 8 },
    { time: "15:00", lessonTypeName: "개인", instructorName: "이수진", reserved: 1, capacity: 1 },
    { time: "15:00", lessonTypeName: "그룹", instructorName: "이수진", reserved: 6, capacity: 8 },
    { time: "17:00", lessonTypeName: "그룹", instructorName: "한가람", reserved: 6, capacity: 8 },
    { time: "18:00", lessonTypeName: "듀엣", instructorName: "박지영", reserved: 1, capacity: 2 },
    { time: "19:00", lessonTypeName: "그룹", instructorName: "윤지민", reserved: 4, capacity: 8 },
    { time: "19:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
    { time: "20:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
  ],
  Fri: [
    { time: "07:00", lessonTypeName: "그룹", instructorName: "최재훈", reserved: 5, capacity: 8 },
    { time: "09:00", lessonTypeName: "그룹", instructorName: "박지영", reserved: 4, capacity: 8 },
    { time: "11:00", lessonTypeName: "그룹", instructorName: "한가람", reserved: 0, capacity: 8 },
    { time: "18:00", lessonTypeName: "그룹", instructorName: "정유진", reserved: 2, capacity: 8 },
    { time: "19:00", lessonTypeName: "듀엣", instructorName: "최재훈", reserved: 0, capacity: 2 },
    { time: "20:00", lessonTypeName: "그룹", instructorName: "오선아", reserved: 4, capacity: 8 },
    { time: "20:00", lessonTypeName: "그룹", instructorName: "박데모", reserved: 0, capacity: 8 },
  ],
  Sat: [
    { time: "09:00", lessonTypeName: "듀엣", instructorName: "이수진", reserved: 0, capacity: 2 },
    { time: "10:00", lessonTypeName: "듀엣", instructorName: "류시현", reserved: 0, capacity: 2 },
    { time: "15:00", lessonTypeName: "듀엣", instructorName: "박지영", reserved: 0, capacity: 2 },
  ],
  Sun: [
    { time: "10:00", lessonTypeName: "듀엣", instructorName: "이수진", reserved: 0, capacity: 2 },
    { time: "11:00", lessonTypeName: "그룹", instructorName: "한가람", reserved: 0, capacity: 8 },
    { time: "14:00", lessonTypeName: "그룹", instructorName: "오선아", reserved: 3, capacity: 8 },
    { time: "15:00", lessonTypeName: "그룹", instructorName: "이수진", reserved: 6, capacity: 8 },
  ],
};
const DOW = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
function makeClassSchedules(fromIso: string, toIso: string) {
  const from = new Date(fromIso);
  const to = new Date(toIso);
  const out = [] as Array<{
    id: number;
    classDate: string;
    startTime: string;
    endTime: string;
    instructorName: string;
    lessonTypeName: string;
    maxCapacity: number;
    currentCount: number;
    status: string;
  }>;
  let id = 5000;
  for (let d = new Date(from); d <= to; d.setDate(d.getDate() + 1)) {
    const key = DOW[d.getDay()];
    const rows = WEEKLY_SCHEDULE[key] || [];
    for (const r of rows) {
      const [hh, mm] = r.time.split(":");
      const endH = String(parseInt(hh, 10)).padStart(2, "0");
      out.push({
        id: ++id,
        classDate: ymd(d),
        startTime: `${hh}:${mm}`,
        endTime: `${endH}:50`,
        instructorName: r.instructorName,
        lessonTypeName: r.lessonTypeName,
        maxCapacity: r.capacity,
        currentCount: r.reserved,
        status: r.reserved >= r.capacity ? "FULL" : "OPEN",
      });
    }
  }
  return out;
}

// ── 강사용 스케줄 (박지영 강사 시점, from~to 범위) ──
// 강사 시연 시 출석 체크가 의미있도록 currentCount > 0 보장
function makeInstructorSchedules(fromIso: string, toIso: string) {
  const all = makeClassSchedules(fromIso, toIso);
  return all
    .filter((s) => s.instructorName === "박지영" || s.instructorName === "박데모")
    .map((s, i) => ({
      ...s,
      reservationId: 9000 + i,
      reservedCount: s.currentCount === 0 ? Math.min(s.maxCapacity, 2 + (i % 4)) : s.currentCount,
      currentCount: s.currentCount === 0 ? Math.min(s.maxCapacity, 2 + (i % 4)) : s.currentCount,
    }));
}

// ── 강사 수업별 출석 명단 (classId → 회원 목록) ──
const ATTENDANCE_NAMES = ["김데모", "홍성민", "유서진", "신연우", "변하림", "정하윤", "박지호", "이서연", "송민재", "조하은"];
function makeAttendancesForClass(classId: number) {
  // classId 기반으로 2~5명 명단 생성, status는 PENDING으로
  const count = 2 + (classId % 4);
  const out = [];
  for (let i = 0; i < count; i++) {
    const name = ATTENDANCE_NAMES[(classId + i) % ATTENDANCE_NAMES.length];
    out.push({
      id: classId * 100 + i,
      reservationId: classId * 100 + i,
      memberId: 1000 + i,
      memberName: name,
      status: "PENDING",
      classScheduleId: classId,
    });
  }
  return out;
}

// ── 설정 (코드의 SETTING_GROUPS와 키가 일치해야 표시됨) ──
let _settingIdSeq = 0;
function S(key: string, value: string, description: string) {
  return { id: ++_settingIdSeq, key, value, description, updatedAt: "2026-05-10T09:00:00" };
}
const SETTINGS = [
  // 예약/취소
  S("CANCEL_DEADLINE_HOURS", "2", "예약 취소 마감 (수업 시작 N시간 전)"),
  S("NO_SHOW_AUTO_MARK_MINUTES", "10", "수업 종료 후 N분 뒤 자동 노쇼 처리"),
  // 정기권
  S("UNLIMITED_MONTHLY_LIMIT", "30", "무제한권 월 최대 예약 횟수"),
  S("MEMBERSHIP_EXPIRY_ALERT_DAYS", "7,3", "만료 D-N일 전 알림 (콤마 구분)"),
  S("MEMBERSHIP_LOW_COUNT_ALERT", "3", "잔여 N회 이하 알림"),
  // 수업
  S("DEFAULT_LESSON_DURATION", "50", "수업 1회 기본 시간 (분)"),
  // 알림
  S("REMINDER_1DAY_HOUR", "19", "전날 리마인더 시각"),
  S("REMINDER_SAME_DAY_HOURS", "1", "당일 리마인더 (수업 N시간 전)"),
  // 기타
  S("STUDIO_NAME", "필라테스 OO점", "스튜디오 이름"),
  S("STUDIO_PHONE", "02-1234-5678", "연락처"),
  S("STUDIO_HOURS", "평일 09:00~21:00 / 토 09:00~17:00", "운영시간"),
  S("STUDIO_HOLIDAY", "일요일, 공휴일", "휴무일"),
];

// ── 알림 ──
const NOTIFICATIONS = [
  { id: 1, title: "내일 듀엣 수업 알림", body: "내일 11:00 박지영 강사 듀엣 수업이 예정되어 있어요.", read: false, createdAt: TODAY + "T09:00:00" },
  { id: 2, title: "수강권 만료 D-59", body: "12회권이 2026-07-09에 만료됩니다.", read: false, createdAt: TODAY + "T08:00:00" },
  { id: 3, title: "출석 체크 완료", body: "5/9 그룹 수업 출석이 기록되었어요.", read: true, createdAt: "2026-05-09T11:00:00" },
];

// ──────────────────────────────────────────────────────────
// 매칭 & 응답 생성
// ──────────────────────────────────────────────────────────
function jsonResponse<T>(config: InternalAxiosRequestConfig, body: T, status = 200): AxiosResponse {
  return {
    data: body,
    status,
    statusText: status === 200 ? "OK" : "",
    headers: {},
    config,
    request: undefined,
  };
}

function b64urlEncode(s: string) {
  // 한글 안전 base64 url
  const b = typeof btoa !== "undefined" ? btoa(unescape(encodeURIComponent(s))) : "";
  return b.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
function makeJwt(payload: Record<string, unknown>) {
  const header = { alg: "none", typ: "JWT" };
  return `${b64urlEncode(JSON.stringify(header))}.${b64urlEncode(JSON.stringify(payload))}.demo`;
}
function makeLoginResponse(role: "MEMBER" | "INSTRUCTOR" | "ADMIN") {
  const sub = role === "MEMBER" ? "MBR-DEMO-0001" : role === "INSTRUCTOR" ? "10" : "1";
  const jwt = makeJwt({
    sub,
    role: role === "ADMIN" ? "SUPER_ADMIN" : role,
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + 3600,
  });
  return {
    accessToken: jwt,
    refreshToken: `demo-refresh-${role.toLowerCase()}-${Date.now()}`,
    expiresIn: 3600,
    ...(role === "ADMIN" ? { adminId: 1, role: "SUPER_ADMIN", instructorId: null } : {}),
    ...(role === "INSTRUCTOR" ? { adminId: 0, role: "INSTRUCTOR", instructorId: 10 } : {}),
  };
}

function mockMatch(config: InternalAxiosRequestConfig): AxiosResponse | null {
  const method = (config.method || "get").toLowerCase();
  const url = (config.url || "").split("?")[0];
  const params = (config.params as Record<string, unknown>) || {};

  // 인증
  if (url === "/api/auth/login" && method === "post") {
    return jsonResponse(config, ok(makeLoginResponse("MEMBER")));
  }
  if (url === "/api/admin/auth/login" && method === "post") {
    // loginId가 instructor_로 시작하면 강사, 아니면 관리자
    try {
      const body = typeof config.data === "string" ? JSON.parse(config.data) : config.data;
      const loginId = String(body?.loginId || "");
      if (loginId.startsWith("instructor")) {
        return jsonResponse(config, ok(makeLoginResponse("INSTRUCTOR")));
      }
    } catch {
      // ignore
    }
    return jsonResponse(config, ok(makeLoginResponse("ADMIN")));
  }
  if (url === "/api/auth/refresh" || url === "/api/admin/auth/refresh") {
    return jsonResponse(config, ok({ accessToken: `demo-refreshed-${Date.now()}`, expiresIn: 3600 }));
  }
  if (url === "/api/auth/sms/request" && method === "post") {
    return jsonResponse(config, ok(undefined));
  }
  if (url === "/api/auth/sms/verify" && method === "post") {
    return jsonResponse(config, ok({ verifiedToken: "demo-verified-token" }));
  }
  if (url === "/api/auth/signup" && method === "post") {
    return jsonResponse(config, ok({ ...makeLoginResponse("MEMBER"), memberId: MEMBER_ME.publicId }));
  }
  if (url === "/api/auth/reset-password" && method === "post") {
    return jsonResponse(config, ok(undefined));
  }

  // 회원
  if (url === "/api/members/me" && method === "get") return jsonResponse(config, ok(MEMBER_ME));
  if (url === "/api/members/me" && method === "delete") return jsonResponse(config, ok(undefined));
  if (url === "/api/members/me/memberships" && method === "get") return jsonResponse(config, ok(MEMBER_MEMBERSHIPS));
  if (url === "/api/members/me/reservations" && method === "get") return jsonResponse(config, ok(MEMBER_RESERVATIONS));
  if (url === "/api/members/me/attendances" && method === "get") return jsonResponse(config, ok(MEMBER_ATTENDANCES));
  if (url === "/api/members/me/memberships/purchase" && method === "post") return jsonResponse(config, ok({ id: 999 }));
  if (url === "/api/membership-passes" && method === "get") return jsonResponse(config, ok(MEMBERSHIP_PASSES));

  // 수업 (회원/공용)
  if (url === "/api/class-schedules" && method === "get") {
    const from = String(params.from || TODAY);
    const to = String(params.to || DAY_AFTER);
    return jsonResponse(config, ok(makeClassSchedules(from, to)));
  }
  if (url === "/api/lesson-types" && method === "get") return jsonResponse(config, ok(LESSON_TYPES));

  // 예약
  if (url === "/api/reservations" && method === "post") return jsonResponse(config, ok({ id: Math.floor(Math.random() * 9000) + 1000 }));
  if (url.startsWith("/api/reservations/") && method === "delete") return jsonResponse(config, ok(undefined));

  // 관리자
  if (url === "/api/admin/dashboard" && method === "get") return jsonResponse(config, ok(DASHBOARD));
  if (url === "/api/admin/dashboard/revenue" && method === "get") {
    const period = String(params.period || "week");
    return jsonResponse(config, ok(makeRevenueForPeriod(period)));
  }
  if (url === "/api/admin/members" && method === "get") {
    const page = Number(params.page ?? 0);
    const size = Number(params.size ?? 10);
    return jsonResponse(config, ok(makeAdminMembers(page, size)));
  }
  if (url === "/api/admin/members" && method === "post") return jsonResponse(config, ok({ id: 999, publicId: "MBR-NEW" }));
  {
    const m = url.match(/^\/api\/admin\/members\/(\d+)$/);
    if (m && method === "get") return jsonResponse(config, ok(makeMemberDetail(Number(m[1]))));
  }
  {
    const m = url.match(/^\/api\/admin\/members\/(\d+)\/memos$/);
    if (m && method === "post") {
      let content = "";
      try {
        const body = typeof config.data === "string" ? JSON.parse(config.data) : config.data;
        content = String(body?.content || "");
      } catch { /* empty */ }
      return jsonResponse(config, ok({
        id: Math.floor(Math.random() * 9000) + 1000,
        content,
        writerName: "데모관리자",
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }));
    }
  }
  if (url.startsWith("/api/admin/members/") && method === "patch") return jsonResponse(config, ok(undefined));
  if (url.startsWith("/api/admin/members/") && method === "delete") return jsonResponse(config, ok(undefined));
  if (url === "/api/admin/members/bulk" && method === "post") return jsonResponse(config, ok({ imported: 0, failed: 0 }));
  if (url === "/api/admin/memberships" && method === "post") return jsonResponse(config, ok({ id: 999 }));

  if (url === "/api/admin/class-schedules" && method === "get") {
    const from = String(params.from || TODAY);
    const to = String(params.to || DAY_AFTER);
    return jsonResponse(config, ok(makeClassSchedules(from, to)));
  }
  if (url === "/api/admin/class-schedules" && method === "post") return jsonResponse(config, ok({ id: 9999 }));
  if (url === "/api/admin/class-schedules/generate" && method === "post") return jsonResponse(config, ok({ createdCount: 84 }));
  if (url.match(/^\/api\/admin\/class-schedules\/\d+\/cancel$/) && method === "post") return jsonResponse(config, ok(undefined));

  if (url === "/api/admin/instructors" && method === "get") return jsonResponse(config, ok(INSTRUCTORS));
  if (url === "/api/admin/instructors" && method === "post") return jsonResponse(config, ok({ id: 99 }));
  if (url.startsWith("/api/admin/instructors/") && method === "patch") return jsonResponse(config, ok(undefined));
  if (url.startsWith("/api/admin/instructors/") && method === "delete") return jsonResponse(config, ok(undefined));

  if (url === "/api/admin/membership-passes" && method === "get") return jsonResponse(config, ok(MEMBERSHIP_PASSES));
  if (url === "/api/admin/membership-passes" && method === "post") return jsonResponse(config, ok({ id: 99 }));
  if (url.startsWith("/api/admin/membership-passes/") && method === "patch") return jsonResponse(config, ok(undefined));
  if (url.startsWith("/api/admin/membership-passes/") && method === "delete") return jsonResponse(config, ok(undefined));

  if (url === "/api/admin/settings" && method === "get") return jsonResponse(config, ok({ settings: SETTINGS }));
  if (url === "/api/admin/settings" && method === "patch") return jsonResponse(config, ok(undefined));

  // 강사
  if (url === "/api/instructor/class-schedules" && method === "get") {
    const from = String(params.from || TODAY);
    const to = String(params.to || TODAY);
    return jsonResponse(config, ok(makeInstructorSchedules(from, to)));
  }
  {
    const m = url.match(/^\/api\/instructor\/class-schedules\/(\d+)\/attendances$/);
    if (m && method === "get") {
      return jsonResponse(config, ok(makeAttendancesForClass(Number(m[1]))));
    }
  }
  if (url.startsWith("/api/instructor/attendances/") && method === "post") return jsonResponse(config, ok(undefined));

  // 알림
  if (url === "/api/members/me/notifications" && method === "get") return jsonResponse(config, ok(NOTIFICATIONS));
  if (url === "/api/notifications" && method === "get") return jsonResponse(config, ok(NOTIFICATIONS));

  return null;
}

/**
 * axios adapter — 모든 요청을 가로채 mock 응답으로 즉시 resolve.
 * 매칭되지 않은 URL은 빈 success 응답으로 처리해 UI가 빈 상태로 자연스럽게 보이게 함.
 */
export const demoMockAdapter: AxiosAdapter = async (config) => {
  // 약간의 latency를 흉내내어 자연스러운 로딩 표시
  await new Promise((r) => setTimeout(r, 80));
  const hit = mockMatch(config);
  if (hit) return hit;
  // 미매칭 fallback: 빈 응답
  return jsonResponse(config, { success: true, data: null, error: null });
};
