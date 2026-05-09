export interface Member {
  publicId: string;
  name: string;
  phoneNumber: string;
  gender: string;
  birthDate: string;
  status: string;
  profileImageUrl: string | null;
  createdAt: string;
}

export interface Membership {
  publicId: string;
  passName: string;
  totalCount: number;
  remainingCount: number;
  unlimited: boolean;
  startDate: string;
  endDate: string;
  status: string;
}

export interface ClassSchedule {
  id: number;
  classDate: string;
  startTime: string;
  endTime: string;
  instructorName: string;
  lessonTypeName: string;
  maxCapacity: number;
  currentCount: number;
  status: string;
}

export interface Reservation {
  id: number;
  classDate: string;
  startTime: string;
  endTime: string;
  instructorName: string;
  lessonTypeName: string;
  status: string;
}

export interface DashboardData {
  todayClasses: {
    count: number;
    schedules: {
      time: string;
      instructor: string;
      className: string;
      reservedCount: number;
      capacity: number;
    }[];
  };
  thisWeekRevenue: {
    total: number;
    breakdown: { date: string; amount: number }[];
  };
  expiringMemberships: {
    memberId: number;
    memberName: string;
    passName: string;
    daysLeft: number;
    endDate: string;
  }[];
  alerts: {
    noShowMembers: { memberId: number; memberName: string; noShowCount: number }[];
    lowMembershipMembers: {
      memberId: number;
      memberName: string;
      passName: string;
      remainingCount: number;
    }[];
  };
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface AdminLoginResponse extends LoginResponse {
  adminId: number;
  role: string;
  instructorId: number | null;
}

export interface SignupResponse extends LoginResponse {
  memberId: string;
}
