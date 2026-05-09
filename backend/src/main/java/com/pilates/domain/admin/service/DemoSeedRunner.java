package com.pilates.domain.admin.service;

import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.common.security.hash.HashingService;
import com.pilates.domain.admin.entity.Admin;
import com.pilates.domain.admin.entity.AdminRole;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.attendance.entity.Attendance;
import com.pilates.domain.attendance.entity.AttendanceStatus;
import com.pilates.domain.attendance.repository.AttendanceRepository;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.entity.ClassScheduleStatus;
import com.pilates.domain.classroom.entity.LessonType;
import com.pilates.domain.classroom.repository.ClassScheduleRepository;
import com.pilates.domain.classroom.repository.LessonTypeRepository;
import com.pilates.domain.instructor.entity.Instructor;
import com.pilates.domain.instructor.entity.InstructorStatus;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.domain.member.entity.Gender;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.entity.MemberStatus;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipPass;
import com.pilates.domain.membership.entity.MembershipStatus;
import com.pilates.domain.membership.repository.MembershipPassRepository;
import com.pilates.domain.membership.repository.MembershipRepository;
import com.pilates.domain.payment.entity.Payment;
import com.pilates.domain.payment.entity.PaymentMethod;
import com.pilates.domain.payment.entity.PaymentStatus;
import com.pilates.domain.payment.repository.PaymentRepository;
import com.pilates.domain.reservation.entity.Reservation;
import com.pilates.domain.reservation.entity.ReservationStatus;
import com.pilates.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 시연용 풍부한 시드 데이터.
 * local 프로파일에서만 실행. 이미 회원 30명 이상이면 skip.
 */
@Slf4j
@Component
@Profile({"local", "local-h2"})
@RequiredArgsConstructor
public class DemoSeedRunner implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final InstructorRepository instructorRepository;
    private final AdminRepository adminRepository;
    private final LessonTypeRepository lessonTypeRepository;
    private final MembershipPassRepository membershipPassRepository;
    private final MembershipRepository membershipRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final EncryptionService encryptionService;
    private final HashingService hashingService;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42);

    private static final String[] NAMES = {
            "김민지", "이서연", "박소윤", "최유진", "정하윤",
            "강하린", "오지원", "윤채원", "한수빈", "임지아",
            "장예은", "송다현", "권나영", "조민아", "유하은",
            "신서현", "문지우", "배수아", "홍가영", "전소미",
            "류서진", "서가은", "안지현", "남은서", "황채림",
            "노유나", "구보라", "양시은", "엄세라", "피지영"
    };

    private static final String[] INSTRUCTOR_NAMES = {"박지영", "이수진", "최재훈", "김하늘", "정유진"};

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.count() >= 10) {
            log.info("시연 시드: 이미 데이터 존재 (회원 {}명), skip", memberRepository.count());
            return;
        }

        log.info("=== 시연용 시드 데이터 생성 시작 ===");

        // 1. 강사 5명
        List<Instructor> instructors = createInstructors();

        // 2. 강사별 admin 계정
        createInstructorAdmins(instructors);

        // 3. 수업 유형 확인
        List<LessonType> lessonTypes = lessonTypeRepository.findAll();
        if (lessonTypes.isEmpty()) {
            log.warn("수업 유형이 없습니다. R__dev_seed.sql이 먼저 실행되어야 합니다.");
            return;
        }

        // 4. 정기권 종류 확인
        List<MembershipPass> passes = membershipPassRepository.findAll();

        // 5. 회원 30명
        List<Member> members = createMembers();

        // 6. 수업 시간표 4주
        List<ClassSchedule> schedules = createSchedules(instructors, lessonTypes);

        // 7. 정기권 발급 + 결제
        List<Membership> memberships = createMembershipsAndPayments(members, passes);

        // 8. 예약 + 출석
        createReservationsAndAttendances(members, memberships, schedules);

        log.info("=== 시연용 시드 완료: 회원 {}명, 강사 {}명, 수업 {}건, 정기권 {}건 ===",
                members.size(), instructors.size(), schedules.size(), memberships.size());
    }

    private List<Instructor> createInstructors() {
        List<Instructor> result = new ArrayList<>();
        for (int i = 0; i < INSTRUCTOR_NAMES.length; i++) {
            final String name = INSTRUCTOR_NAMES[i];
            String phone = String.format("010%04d%04d", 1000 + i, 1000 + i);
            if (instructorRepository.findAll().stream()
                    .anyMatch(ins -> name.equals(ins.getName()) && ins.getDeletedAt() == null)) {
                result.addAll(instructorRepository.findAll().stream()
                        .filter(ins -> name.equals(ins.getName()))
                        .toList());
                continue;
            }
            Instructor ins = Instructor.builder()
                    .publicId(UUID.randomUUID().toString().replace("-", ""))
                    .name(INSTRUCTOR_NAMES[i])
                    .phoneEncrypted(encryptionService.encrypt(phone))
                    .phoneHash(hashingService.hash(phone))
                    .status(InstructorStatus.ACTIVE)
                    .build();
            result.add(instructorRepository.save(ins));
        }
        return result;
    }

    private void createInstructorAdmins(List<Instructor> instructors) {
        String hash = passwordEncoder.encode("admin1234");
        for (Instructor ins : instructors) {
            String loginId = "inst_" + ins.getName().replace(" ", "");
            if (adminRepository.findByLoginIdAndDeletedAtIsNull(loginId).isEmpty()) {
                adminRepository.save(Admin.builder()
                        .loginId(loginId)
                        .passwordHash(hash)
                        .name(ins.getName())
                        .role(AdminRole.INSTRUCTOR)
                        .instructor(ins)
                        .active(true)
                        .build());
            }
        }
    }

    private List<Member> createMembers() {
        List<Member> result = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            String phone = String.format("010%04d%04d", 9000 + i, 1000 + i);
            String phoneHash = hashingService.hash(phone);
            if (memberRepository.existsByPhoneHashAndDeletedAtIsNull(phoneHash)) continue;

            Member m = Member.builder()
                    .publicId(UUID.randomUUID().toString().replace("-", ""))
                    .name(encryptionService.encrypt(NAMES[i]))
                    .phoneEncrypted(encryptionService.encrypt(phone))
                    .phoneHash(phoneHash)
                    .gender(random.nextBoolean() ? Gender.FEMALE : Gender.MALE)
                    .status(MemberStatus.ACTIVE)
                    .passwordHash(passwordEncoder.encode("Test1234!"))
                    .build();
            result.add(memberRepository.save(m));
        }
        // 2명 탈퇴 처리
        if (result.size() >= 28) {
            result.get(28).withdraw();
            result.get(29).withdraw();
        }
        return result;
    }

    private List<ClassSchedule> createSchedules(List<Instructor> instructors, List<LessonType> lessonTypes) {
        List<ClassSchedule> result = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(14);
        LocalDate end = LocalDate.now().plusDays(14);
        LocalTime[] times = {LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(14, 0), LocalTime.of(19, 0)};
        int insIdx = 0;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            // 일요일도 2슬롯 (시연용)
            int slotsPerDay = (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) ? 2 : 4;
            for (int s = 0; s < slotsPerDay; s++) {
                LessonType lt = lessonTypes.get(random.nextInt(Math.min(3, lessonTypes.size())));
                Instructor ins = instructors.get(insIdx % instructors.size());
                insIdx++;

                ClassSchedule cs = ClassSchedule.builder()
                        .instructor(ins)
                        .lessonType(lt)
                        .classDate(d)
                        .startTime(times[s])
                        .endTime(times[s].plusMinutes(50))
                        .maxCapacity(lt.getMaxCapacity())
                        .status(ClassScheduleStatus.SCHEDULED)
                        .build();
                result.add(classScheduleRepository.save(cs));
            }
        }
        // 3건 휴강
        for (int i = 0; i < 3 && i < result.size(); i++) {
            if (result.get(i).getClassDate().isBefore(LocalDate.now())) {
                result.get(i).cancel();
            }
        }
        return result;
    }

    private List<Membership> createMembershipsAndPayments(List<Member> members, List<MembershipPass> passes) {
        List<Membership> result = new ArrayList<>();
        if (passes.isEmpty()) return result;

        for (int i = 0; i < Math.min(members.size(), 28); i++) {
            Member m = members.get(i);
            if (m.getStatus() == MemberStatus.WITHDRAWN) continue;

            MembershipPass pass = passes.get(random.nextInt(passes.size()));
            int totalCount = pass.getTotalCount() != null ? pass.getTotalCount() : 30;
            int remaining = Math.max(1, random.nextInt(totalCount + 1));
            LocalDate startDate = LocalDate.now().minusDays(random.nextInt(60));
            LocalDate endDate = startDate.plusDays(pass.getValidityDays());
            MembershipStatus status = endDate.isBefore(LocalDate.now()) ? MembershipStatus.EXPIRED
                    : remaining == 0 ? MembershipStatus.EXHAUSTED : MembershipStatus.ACTIVE;

            // 만료 임박 5건
            if (i >= 20 && i < 25) {
                endDate = LocalDate.now().plusDays(random.nextInt(7));
                status = MembershipStatus.ACTIVE;
                remaining = random.nextInt(3) + 1;
            }
            // 잔여 1회 이하 5건
            if (i >= 25 && i < 28) {
                remaining = random.nextInt(2);
                status = remaining == 0 ? MembershipStatus.EXHAUSTED : MembershipStatus.ACTIVE;
                endDate = LocalDate.now().plusDays(30);
            }

            Membership ms = Membership.builder()
                    .publicId(UUID.randomUUID().toString().replace("-", ""))
                    .member(m)
                    .totalCount(totalCount)
                    .remainingCount(remaining)
                    .unlimited(pass.isUnlimited())
                    .startDate(startDate)
                    .endDate(endDate)
                    .price(pass.getPrice())
                    .status(status)
                    .membershipPass(pass)
                    .build();
            result.add(membershipRepository.save(ms));

            // 결제
            Payment payment = Payment.builder()
                    .orderId("demo_" + UUID.randomUUID().toString().substring(0, 8))
                    .member(m)
                    .membershipPass(pass)
                    .amount(pass.getPrice())
                    .method(PaymentMethod.CARD)
                    .status(PaymentStatus.PENDING)
                    .build();
            payment.confirm("mock_pk_demo_" + i, "카드", startDate.atTime(12, 0));
            payment.linkMembership(ms);
            paymentRepository.save(payment);
        }
        return result;
    }

    private void createReservationsAndAttendances(List<Member> members, List<Membership> memberships, List<ClassSchedule> schedules) {
        List<Member> activeMembers = members.stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .toList();
        Map<Long, Membership> membershipMap = new HashMap<>();
        for (Membership ms : memberships) {
            if (ms.getStatus() == MembershipStatus.ACTIVE || ms.getStatus() == MembershipStatus.EXHAUSTED) {
                membershipMap.putIfAbsent(ms.getMember().getId(), ms);
            }
        }

        int resCount = 0;
        for (ClassSchedule cs : schedules) {
            if (cs.getStatus() == ClassScheduleStatus.CANCELLED) continue;
            int slots = Math.max(1, (int) (cs.getMaxCapacity() * (0.5 + random.nextDouble() * 0.4)));
            Set<Long> booked = new HashSet<>();

            for (int s = 0; s < slots && s < activeMembers.size(); s++) {
                Member m = activeMembers.get((resCount + s) % activeMembers.size());
                if (booked.contains(m.getId())) continue;
                Membership ms = membershipMap.get(m.getId());
                if (ms == null) continue;

                booked.add(m.getId());

                Reservation res = Reservation.builder()
                        .member(m)
                        .classSchedule(cs)
                        .membership(ms)
                        .status(ReservationStatus.CONFIRMED)
                        .build();

                // 과거 수업: 일부 취소/노쇼
                if (cs.getClassDate().isBefore(LocalDate.now())) {
                    int roll = random.nextInt(100);
                    if (roll < 5) {
                        res = Reservation.builder().member(m).classSchedule(cs).membership(ms)
                                .status(ReservationStatus.CANCELLED).build();
                    } else if (roll < 8) {
                        res = Reservation.builder().member(m).classSchedule(cs).membership(ms)
                                .status(ReservationStatus.NO_SHOW).build();
                    }
                }

                Reservation saved = reservationRepository.save(res);

                // 출석 기록 (과거 수업만)
                if (cs.getClassDate().isBefore(LocalDate.now()) && saved.getStatus() == ReservationStatus.CONFIRMED) {
                    AttendanceStatus attStatus;
                    int attRoll = random.nextInt(100);
                    if (attRoll < 88) attStatus = AttendanceStatus.ATTENDED;
                    else if (attRoll < 93) attStatus = AttendanceStatus.LATE;
                    else if (attRoll < 97) attStatus = AttendanceStatus.ABSENT;
                    else attStatus = AttendanceStatus.NO_SHOW;

                    Attendance att = Attendance.builder()
                            .reservation(saved)
                            .member(m)
                            .classSchedule(cs)
                            .status(attStatus)
                            .build();
                    attendanceRepository.save(att);
                } else if (cs.getClassDate().equals(LocalDate.now()) || cs.getClassDate().isAfter(LocalDate.now())) {
                    // 미래/오늘 수업: PENDING
                    Attendance att = Attendance.createPending(saved);
                    attendanceRepository.save(att);
                }

                // currentCount 갱신
                if (saved.getStatus() == ReservationStatus.CONFIRMED) {
                    try { cs.incrementCount(); } catch (Exception ignored) {}
                }

                resCount++;
            }
        }
        log.info("예약 {}건, 출석 {}건 생성", reservationRepository.count(), attendanceRepository.count());
    }
}
