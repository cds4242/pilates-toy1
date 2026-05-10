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
 * local 프로파일에서만 실행. 빈 DB일 때 풍성한 demo 데이터 일괄 생성.
 */
@Slf4j
@Component
@Profile({"local", "local-h2", "portfolio"})
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

    /** 80명 회원 — 다양한 사용 패턴(신규/단골/만료임박/만료/노쇼다발 등). */
    private static final String[] NAMES = {
            // 활성 단골 (1~25)
            "김민지", "이서연", "박소윤", "최유진", "정하윤",
            "강하린", "오지원", "윤채원", "한수빈", "임지아",
            "장예은", "송다현", "권나영", "조민아", "유하은",
            "신서현", "문지우", "배수아", "홍가영", "전소미",
            "류서진", "서가은", "안지현", "남은서", "황채림",
            // 만료 임박 / 잔여 부족 (26~40)
            "노유나", "구보라", "양시은", "엄세라", "피지영",
            "차하은", "백서윤", "성지원", "권유진", "도지아",
            "한채영", "신지수", "주서영", "변하림", "원시현",
            // 만료/탈퇴/장기미사용 (41~55)
            "고나현", "심예진", "기은채", "태유나", "주아라",
            "박은서", "정채원", "조서연", "이가람", "최은비",
            "강예진", "오시현", "유민지", "송하영", "임소율",
            // 신규 가입 (56~70)
            "김도윤", "박서준", "최우진", "이도현", "정시우",
            "강민준", "오은우", "윤재이", "한이찬", "임주안",
            "장하준", "송예준", "권시온", "조유준", "유서진",
            // 체험/일회성 (71~80)
            "신연우", "문건우", "배지호", "홍성민", "전현우",
            "류지훈", "서한결", "안주원", "남예준", "황태민"
    };

    /** 9명 강사 — 다양한 전문성. */
    private static final String[] INSTRUCTOR_NAMES = {
            "박지영", "이수진", "최재훈", "김하늘", "정유진",
            "한가람", "오선아", "윤지민", "류시현"
    };

    private static final String[] INSTRUCTOR_SPECIALTIES = {
            "리포머 · 매트", "재활 필라테스", "그룹 다이나믹",
            "사전후 케어", "체형교정", "코어 강화",
            "임산부 케어", "시니어", "리포머 · 차이체어"
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.count() >= 70) {
            log.info("시연 시드: 이미 충분한 데이터 존재 (회원 {}명), 대량 시드는 skip",
                    memberRepository.count());
            // 데모 계정 3개는 idempotent하게 보장 (재기동 시 누락 방지)
            try {
                createDemoShowcaseAccounts(
                        instructorRepository.findAll(),
                        lessonTypeRepository.findAll(),
                        membershipPassRepository.findAll(),
                        classScheduleRepository.findAll());
            } catch (Exception e) {
                log.warn("데모 계정 보강 실패: {}", e.getMessage());
            }
            return;
        }

        log.info("=== 시연용 풍부한 시드 데이터 생성 시작 ===");

        // 1. 강사 9명
        List<Instructor> instructors = createInstructors();

        // 2. 강사별 admin 계정
        createInstructorAdmins(instructors);

        // 3. 수업 유형 확인 (없으면 portfolio 등 SQL 시드가 안 도는 환경을 위해 자바에서 직접 생성)
        List<LessonType> lessonTypes = lessonTypeRepository.findAll();
        if (lessonTypes.isEmpty()) {
            log.info("수업 유형이 비어있어 자바 코드로 기본값 4종을 시드합니다");
            lessonTypes = createDefaultLessonTypes();
        }

        // 4. 정기권 종류 확인 (없으면 자바에서 기본 4종 생성)
        List<MembershipPass> passes = membershipPassRepository.findAll();
        if (passes.isEmpty()) {
            log.info("정기권이 비어있어 자바 코드로 기본 4종을 시드합니다");
            passes = createDefaultMembershipPasses();
        }

        // 4-1. 시스템 관리자 admin 보강 (R__dev_seed.sql이 안 돈 경우 대비)
        if (adminRepository.findByLoginIdAndDeletedAtIsNull("admin").isEmpty()) {
            adminRepository.save(Admin.builder()
                    .loginId("admin")
                    .passwordHash(passwordEncoder.encode("demo1234"))
                    .name("시스템관리자")
                    .role(AdminRole.SUPER_ADMIN)
                    .active(true)
                    .build());
            log.info("기본 관리자 계정 생성: admin / demo1234");
        }

        // 5. 회원 80명
        List<Member> members = createMembers();

        // 6. 수업 시간표 12주 (과거 6주 + 미래 6주)
        List<ClassSchedule> schedules = createSchedules(instructors, lessonTypes);

        // 7. 정기권 발급 + 결제 (회원당 평균 1.4건 — 일부는 갱신/연장 이력 보유)
        List<Membership> memberships = createMembershipsAndPayments(members, passes);

        // 8. 예약 + 출석
        createReservationsAndAttendances(members, memberships, schedules);

        // 9. 포트폴리오 시연용 대표 계정 3개 (회원/강사/관리자)
        createDemoShowcaseAccounts(instructors, lessonTypes,
                membershipPassRepository.findAll(), schedules);

        log.info("=== 시연용 시드 완료: 회원 {}명, 강사 {}명, 수업 {}건, 정기권 {}건, 예약 {}건, 결제 {}건 ===",
                memberRepository.count(),
                instructorRepository.count(),
                classScheduleRepository.count(),
                membershipRepository.count(),
                reservationRepository.count(),
                paymentRepository.count());
    }

    /**
     * 의뢰인 시연용 대표 계정 3개 + 풍성한 연관 데이터.
     * 로그인 화면 "데모 계정 카드"에서 클릭 한 번으로 진입할 수 있게 한다.
     *
     * - 회원: 010-0000-0001 / demo1234 (12회권 보유 + 미래 예약 3건 + 과거 출석)
     * - 강사: instructor_demo / demo1234 (담당 수업 5건 + 예약자 풍부)
     * - 관리자: admin_demo / demo1234 (SUPER_ADMIN, 모든 데이터 접근)
     */
    private void createDemoShowcaseAccounts(List<Instructor> instructors,
                                             List<LessonType> lessonTypes,
                                             List<MembershipPass> passes,
                                             List<ClassSchedule> schedules) {
        log.info("=== 포트폴리오 데모 계정 시드 시작 ===");

        String demoPassword = passwordEncoder.encode("demo1234");

        // 1) 데모 관리자 (admin_demo)
        if (adminRepository.findByLoginIdAndDeletedAtIsNull("admin_demo").isEmpty()) {
            adminRepository.save(Admin.builder()
                    .loginId("admin_demo")
                    .passwordHash(demoPassword)
                    .name("데모관리자")
                    .role(AdminRole.SUPER_ADMIN)
                    .active(true)
                    .build());
            log.info("데모 관리자 생성: admin_demo / demo1234");
        }

        // 2) 데모 강사 (Instructor 엔티티 + Admin 매핑)
        Instructor demoInstructor = instructorRepository.findAll().stream()
                .filter(ins -> "박데모".equals(ins.getName()) && ins.getDeletedAt() == null)
                .findFirst()
                .orElseGet(() -> {
                    String phone = "01000000002";
                    Instructor ins = Instructor.builder()
                            .publicId("demo_instructor_" + UUID.randomUUID().toString().substring(0, 8))
                            .name("박데모")
                            .phoneEncrypted(encryptionService.encrypt(phone))
                            .phoneHash(hashingService.hash(phone))
                            .status(InstructorStatus.ACTIVE)
                            .build();
                    return instructorRepository.save(ins);
                });

        if (adminRepository.findByLoginIdAndDeletedAtIsNull("instructor_demo").isEmpty()) {
            adminRepository.save(Admin.builder()
                    .loginId("instructor_demo")
                    .passwordHash(demoPassword)
                    .name("박데모")
                    .role(AdminRole.INSTRUCTOR)
                    .instructor(demoInstructor)
                    .active(true)
                    .build());
            log.info("데모 강사 생성: instructor_demo / demo1234");
        }

        // 데모 강사가 담당할 미래 수업 5건 추가 (오늘~7일 내)
        List<ClassSchedule> demoInstructorSchedules = new ArrayList<>();
        if (!lessonTypes.isEmpty()) {
            LessonType groupType = lessonTypes.stream()
                    .filter(lt -> "그룹".equals(lt.getName())).findFirst()
                    .orElse(lessonTypes.get(0));
            LocalTime[] slots = {
                    LocalTime.of(10, 0), LocalTime.of(14, 0),
                    LocalTime.of(18, 0), LocalTime.of(19, 0), LocalTime.of(20, 0)
            };
            for (int i = 0; i < 5; i++) {
                LocalDate date = LocalDate.now().plusDays(i);
                LocalTime time = slots[i];
                // 같은 강사+날짜+시간 중복 방지
                boolean exists = classScheduleRepository.findAll().stream().anyMatch(cs ->
                        cs.getInstructor() != null
                                && cs.getInstructor().getId().equals(demoInstructor.getId())
                                && date.equals(cs.getClassDate())
                                && time.equals(cs.getStartTime())
                                && cs.getStatus() != ClassScheduleStatus.CANCELLED);
                if (exists) continue;
                ClassSchedule cs = ClassSchedule.builder()
                        .instructor(demoInstructor)
                        .lessonType(groupType)
                        .classDate(date)
                        .startTime(time)
                        .endTime(time.plusMinutes(50))
                        .maxCapacity(groupType.getMaxCapacity())
                        .status(ClassScheduleStatus.SCHEDULED)
                        .build();
                demoInstructorSchedules.add(classScheduleRepository.save(cs));
            }
            log.info("데모 강사 수업 {}건 추가", demoInstructorSchedules.size());
        }

        // 3) 데모 회원 (김데모, 010-0000-0001 / demo1234)
        String demoPhone = "01000000001";
        String demoPhoneHash = hashingService.hash(demoPhone);
        Member demoMember = memberRepository.findByPhoneHashAndDeletedAtIsNull(demoPhoneHash)
                .orElseGet(() -> {
                    Member m = Member.builder()
                            .publicId("demo_member_" + UUID.randomUUID().toString().substring(0, 8))
                            .name(encryptionService.encrypt("김데모"))
                            .phoneEncrypted(encryptionService.encrypt(demoPhone))
                            .phoneHash(demoPhoneHash)
                            .gender(Gender.FEMALE)
                            .status(MemberStatus.ACTIVE)
                            .passwordHash(demoPassword)
                            .build();
                    return memberRepository.save(m);
                });
        log.info("데모 회원 생성: 010-0000-0001 / demo1234");

        // 데모 회원의 12회권 발급 (잔여 8회, 만료 60일 후)
        Membership demoMembership = null;
        if (!passes.isEmpty()) {
            MembershipPass twelveCount = passes.stream()
                    .filter(p -> p.getTotalCount() != null && p.getTotalCount() == 12)
                    .findFirst()
                    .orElse(passes.get(0));
            // 이미 활성 정기권이 있으면 새로 만들지 않음
            boolean alreadyHas = membershipRepository.findAll().stream().anyMatch(ms ->
                    ms.getMember() != null
                            && ms.getMember().getId().equals(demoMember.getId())
                            && ms.getStatus() == MembershipStatus.ACTIVE);
            if (!alreadyHas) {
                LocalDate startDate = LocalDate.now().minusDays(15);
                LocalDate endDate = LocalDate.now().plusDays(60);
                demoMembership = Membership.builder()
                        .publicId("demo_ms_" + UUID.randomUUID().toString().substring(0, 8))
                        .member(demoMember)
                        .totalCount(12)
                        .remainingCount(8)
                        .unlimited(false)
                        .startDate(startDate)
                        .endDate(endDate)
                        .price(twelveCount.getPrice())
                        .status(MembershipStatus.ACTIVE)
                        .membershipPass(twelveCount)
                        .build();
                demoMembership = membershipRepository.save(demoMembership);

                Payment payment = Payment.builder()
                        .orderId("demo_member_" + UUID.randomUUID().toString().substring(0, 8))
                        .member(demoMember)
                        .membershipPass(twelveCount)
                        .amount(twelveCount.getPrice())
                        .method(PaymentMethod.CARD)
                        .status(PaymentStatus.PENDING)
                        .build();
                payment.confirm("mock_pk_demo_" + UUID.randomUUID().toString().substring(0, 8),
                        "카드", startDate.atTime(11, 0));
                payment.linkMembership(demoMembership);
                paymentRepository.save(payment);
            } else {
                demoMembership = membershipRepository.findAll().stream()
                        .filter(ms -> ms.getMember() != null
                                && ms.getMember().getId().equals(demoMember.getId())
                                && ms.getStatus() == MembershipStatus.ACTIVE)
                        .findFirst().orElse(null);
            }
        }

        // 데모 회원의 미래 예약 3건 (서로 다른 날짜/강사)
        if (demoMembership != null && !schedules.isEmpty()) {
            List<ClassSchedule> futureSchedules = schedules.stream()
                    .filter(cs -> cs.getStatus() != ClassScheduleStatus.CANCELLED
                            && !cs.getClassDate().isBefore(LocalDate.now())
                            && !cs.getClassDate().isAfter(LocalDate.now().plusDays(10)))
                    .toList();
            int picked = 0;
            Set<LocalDate> usedDates = new HashSet<>();
            Set<Long> usedInstructorIds = new HashSet<>();
            for (ClassSchedule cs : futureSchedules) {
                if (picked >= 3) break;
                if (usedDates.contains(cs.getClassDate())) continue;
                if (cs.getInstructor() != null && usedInstructorIds.contains(cs.getInstructor().getId())) continue;
                // 이미 예약된 회원이면 skip
                boolean already = reservationRepository.findAll().stream().anyMatch(r ->
                        r.getMember() != null
                                && r.getMember().getId().equals(demoMember.getId())
                                && r.getClassSchedule() != null
                                && r.getClassSchedule().getId().equals(cs.getId())
                                && r.getStatus() == ReservationStatus.CONFIRMED);
                if (already) continue;
                Reservation res = Reservation.builder()
                        .member(demoMember)
                        .classSchedule(cs)
                        .membership(demoMembership)
                        .status(ReservationStatus.CONFIRMED)
                        .build();
                reservationRepository.save(res);
                try { cs.incrementCount(); } catch (Exception ignored) {}
                usedDates.add(cs.getClassDate());
                if (cs.getInstructor() != null) usedInstructorIds.add(cs.getInstructor().getId());
                picked++;
            }
            log.info("데모 회원 미래 예약 {}건 추가", picked);
        }

        log.info("=== 포트폴리오 데모 계정 시드 완료 ===");
    }

    private List<Instructor> createInstructors() {
        List<Instructor> result = new ArrayList<>();
        for (int i = 0; i < INSTRUCTOR_NAMES.length; i++) {
            final String name = INSTRUCTOR_NAMES[i];
            String phone = String.format("010%04d%04d", 1000 + i, 1000 + i);
            Optional<Instructor> existing = instructorRepository.findAll().stream()
                    .filter(ins -> name.equals(ins.getName()) && ins.getDeletedAt() == null)
                    .findFirst();
            if (existing.isPresent()) {
                result.add(existing.get());
                continue;
            }
            Instructor ins = Instructor.builder()
                    .publicId(UUID.randomUUID().toString().replace("-", ""))
                    .name(name)
                    .phoneEncrypted(encryptionService.encrypt(phone))
                    .phoneHash(hashingService.hash(phone))
                    .status(InstructorStatus.ACTIVE)
                    .build();
            result.add(instructorRepository.save(ins));
        }
        return result;
    }

    private void createInstructorAdmins(List<Instructor> instructors) {
        String hash = passwordEncoder.encode("demo1234");
        for (int i = 0; i < instructors.size(); i++) {
            Instructor ins = instructors.get(i);
            String loginId = "instructor" + (i + 1);
            Optional<Admin> existingAdmin = adminRepository.findByLoginIdAndDeletedAtIsNull(loginId);
            if (existingAdmin.isPresent()) {
                // 기존 admin 인스트럭터 링크가 비어 있으면 보정
                Admin a = existingAdmin.get();
                if (a.getInstructor() == null) {
                    a.linkInstructor(ins);
                    adminRepository.save(a);
                    log.info("Admin {} → instructor {} 링크 보정", loginId, ins.getName());
                }
                continue;
            }
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

    private List<Member> createMembers() {
        List<Member> result = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            String phone = String.format("010%04d%04d", 9000 + i, 1000 + i);
            String phoneHash = hashingService.hash(phone);
            if (memberRepository.existsByPhoneHashAndDeletedAtIsNull(phoneHash)) continue;

            // 성별: 70% 여성, 30% 남성
            Gender gender = random.nextInt(100) < 70 ? Gender.FEMALE : Gender.MALE;

            Member m = Member.builder()
                    .publicId(UUID.randomUUID().toString().replace("-", ""))
                    .name(encryptionService.encrypt(NAMES[i]))
                    .phoneEncrypted(encryptionService.encrypt(phone))
                    .phoneHash(phoneHash)
                    .gender(gender)
                    .status(MemberStatus.ACTIVE)
                    .passwordHash(passwordEncoder.encode("demo1234"))
                    .build();
            result.add(memberRepository.save(m));
        }
        // 50~54번 회원은 탈퇴 처리 (5명)
        for (int i = 50; i < 55 && i < result.size(); i++) {
            result.get(i).withdraw();
        }
        return result;
    }

    private List<ClassSchedule> createSchedules(List<Instructor> instructors, List<LessonType> lessonTypes) {
        List<ClassSchedule> result = new ArrayList<>();
        // 과거 6주 + 미래 6주 = 12주
        LocalDate start = LocalDate.now().minusDays(42);
        LocalDate end = LocalDate.now().plusDays(42);

        // 시간 슬롯 풀 (피크/오프피크 구분)
        LocalTime[] weekdayTimes = {
                LocalTime.of(7, 0), LocalTime.of(9, 0), LocalTime.of(10, 0),
                LocalTime.of(11, 0), LocalTime.of(12, 0), LocalTime.of(14, 0),
                LocalTime.of(15, 0), LocalTime.of(17, 0), LocalTime.of(18, 0),
                LocalTime.of(19, 0), LocalTime.of(20, 0)
        };
        LocalTime[] weekendTimes = {
                LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
                LocalTime.of(14, 0), LocalTime.of(15, 0)
        };

        // 강사별 비중: 첫 3명은 풀타임, 나머지는 부분
        // lessonType 가중치: 그룹 50%, 개인 25%, 듀엣 20%, 체험 5%
        double[] lessonWeights = new double[lessonTypes.size()];
        for (int i = 0; i < lessonTypes.size(); i++) {
            String n = lessonTypes.get(i).getName();
            if (n.contains("그룹")) lessonWeights[i] = 50;
            else if (n.contains("개인")) lessonWeights[i] = 25;
            else if (n.contains("듀엣")) lessonWeights[i] = 20;
            else lessonWeights[i] = 5; // 체험
        }
        double weightSum = 0;
        for (double w : lessonWeights) weightSum += w;

        int insIdx = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            LocalTime[] times = isWeekend ? weekendTimes : weekdayTimes;

            // 평일 5~7개, 주말 3~4개
            int slotsPerDay = isWeekend ? 3 + random.nextInt(2) : 5 + random.nextInt(3);
            slotsPerDay = Math.min(slotsPerDay, times.length);

            // 슬롯 시간 추출 (랜덤하게 일부 시간 선택)
            List<LocalTime> selectedTimes = new ArrayList<>(Arrays.asList(times));
            Collections.shuffle(selectedTimes, random);
            selectedTimes = selectedTimes.subList(0, slotsPerDay);
            selectedTimes.sort(Comparator.naturalOrder());

            for (LocalTime time : selectedTimes) {
                // lessonType 가중 추첨
                double r = random.nextDouble() * weightSum;
                double cum = 0;
                LessonType lt = lessonTypes.get(0);
                for (int i = 0; i < lessonTypes.size(); i++) {
                    cum += lessonWeights[i];
                    if (r <= cum) {
                        lt = lessonTypes.get(i);
                        break;
                    }
                }

                // 강사 선택: 처음 3명이 60%, 나머지가 40%
                Instructor ins;
                if (random.nextInt(100) < 60) {
                    ins = instructors.get(insIdx % Math.min(3, instructors.size()));
                } else {
                    ins = instructors.get(3 + (insIdx % Math.max(1, instructors.size() - 3)));
                }
                insIdx++;

                ClassSchedule cs = ClassSchedule.builder()
                        .instructor(ins)
                        .lessonType(lt)
                        .classDate(d)
                        .startTime(time)
                        .endTime(time.plusMinutes(50))
                        .maxCapacity(lt.getMaxCapacity())
                        .status(ClassScheduleStatus.SCHEDULED)
                        .build();
                result.add(classScheduleRepository.save(cs));
            }
        }

        // 과거 5건 휴강 처리
        int cancelled = 0;
        for (ClassSchedule cs : result) {
            if (cancelled >= 5) break;
            if (cs.getClassDate().isBefore(LocalDate.now().minusDays(7)) && random.nextInt(50) == 0) {
                cs.cancel();
                cancelled++;
            }
        }
        return result;
    }

    private List<Membership> createMembershipsAndPayments(List<Member> members, List<MembershipPass> passes) {
        List<Membership> result = new ArrayList<>();
        if (passes.isEmpty()) return result;

        PaymentMethod[] methods = {
                PaymentMethod.CARD, PaymentMethod.CARD, PaymentMethod.CARD,
                PaymentMethod.CARD, PaymentMethod.TRANSFER, PaymentMethod.CASH
        };

        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            if (m.getStatus() == MemberStatus.WITHDRAWN) {
                // 탈퇴 회원도 과거 결제 이력 1건 남기기
                MembershipPass pass = passes.get(random.nextInt(passes.size()));
                createExpiredHistoricalMembership(m, pass, methods, 90, 30);
                continue;
            }

            MembershipPass pass = passes.get(i % passes.size());
            int totalCount = pass.getTotalCount() != null ? pass.getTotalCount() : 30;

            // 패턴별 분포
            // 0~24 (25명): 활성, 단골, 잔여 충분
            // 25~34 (10명): 만료 임박 (D-0 ~ D-7)
            // 35~39 (5명):  잔여 1~2회
            // 40~44 (5명):  최근 만료 (10~30일 전)
            // 45~49 (5명):  장기 만료 + 갱신 (이력 2건)
            // 55~64 (10명): 신규 가입 (시작 0~7일 전)
            // 65~74 (10명): 체험 후 정기권 (체험 + 정기권 2건)
            // 75~79 (5명):  체험만 (1건)

            LocalDate startDate;
            LocalDate endDate;
            int remaining;
            MembershipStatus status = MembershipStatus.ACTIVE;

            if (i < 25) {
                // 활성 단골
                startDate = LocalDate.now().minusDays(20 + random.nextInt(40));
                endDate = startDate.plusDays(pass.getValidityDays());
                remaining = Math.max(3, totalCount - random.nextInt(totalCount / 2 + 1));
            } else if (i < 35) {
                // 만료 임박
                int daysToExpire = random.nextInt(8); // 0 ~ 7
                endDate = LocalDate.now().plusDays(daysToExpire);
                startDate = endDate.minusDays(pass.getValidityDays());
                remaining = random.nextInt(4) + 1;
            } else if (i < 40) {
                // 잔여 1~2회 + 30일 이상 남음
                startDate = LocalDate.now().minusDays(20 + random.nextInt(20));
                endDate = startDate.plusDays(pass.getValidityDays());
                remaining = random.nextInt(2) + 1;
            } else if (i < 45) {
                // 최근 만료 (10~30일 전)
                endDate = LocalDate.now().minusDays(10 + random.nextInt(20));
                startDate = endDate.minusDays(pass.getValidityDays());
                remaining = 0;
                status = MembershipStatus.EXPIRED;
            } else if (i < 50) {
                // 장기 만료 + 갱신 이력
                endDate = LocalDate.now().minusDays(60 + random.nextInt(60));
                startDate = endDate.minusDays(pass.getValidityDays());
                remaining = 0;
                status = MembershipStatus.EXPIRED;
            } else if (i < 55) {
                // 탈퇴 (위에서 처리)
                continue;
            } else if (i < 65) {
                // 신규 가입 (1~7일 전)
                startDate = LocalDate.now().minusDays(random.nextInt(8));
                endDate = startDate.plusDays(pass.getValidityDays());
                remaining = totalCount - random.nextInt(2);
            } else if (i < 75) {
                // 체험 후 정기권 (활성)
                startDate = LocalDate.now().minusDays(15 + random.nextInt(30));
                endDate = startDate.plusDays(pass.getValidityDays());
                remaining = totalCount - random.nextInt(totalCount / 3 + 1);
            } else {
                // 체험만 — 정기권 미보유 (skip 후 체험 정기권 1건)
                MembershipPass trialPass = passes.stream()
                        .filter(p -> p.getName().contains("체험") || p.getTotalCount() != null && p.getTotalCount() <= 2)
                        .findFirst()
                        .orElse(passes.get(0));
                createExpiredHistoricalMembership(m, trialPass, methods, 30, 7);
                continue;
            }

            Membership ms = Membership.builder()
                    .publicId(UUID.randomUUID().toString().replace("-", ""))
                    .member(m)
                    .totalCount(totalCount)
                    .remainingCount(pass.isUnlimited() ? 999 : remaining)
                    .unlimited(pass.isUnlimited())
                    .startDate(startDate)
                    .endDate(endDate)
                    .price(pass.getPrice())
                    .status(status)
                    .membershipPass(pass)
                    .build();
            result.add(membershipRepository.save(ms));

            Payment payment = Payment.builder()
                    .orderId("demo_" + UUID.randomUUID().toString().substring(0, 8))
                    .member(m)
                    .membershipPass(pass)
                    .amount(pass.getPrice())
                    .method(methods[random.nextInt(methods.length)])
                    .status(PaymentStatus.PENDING)
                    .build();
            payment.confirm("mock_pk_" + UUID.randomUUID().toString().substring(0, 8),
                    "카드", startDate.atTime(10 + random.nextInt(8), random.nextInt(60)));
            payment.linkMembership(ms);
            paymentRepository.save(payment);

            // 갱신 이력 (45~49번 회원 + 65~74번 회원)
            if ((i >= 45 && i < 50) || (i >= 65 && i < 75)) {
                MembershipPass renewPass = passes.get((i + 3) % passes.size());
                createRenewalMembership(m, renewPass, methods, endDate);
            }
        }
        return result;
    }

    private void createExpiredHistoricalMembership(Member m, MembershipPass pass,
                                                    PaymentMethod[] methods, int minDaysAgo, int maxDaysAgo) {
        int daysAgo = minDaysAgo + random.nextInt(maxDaysAgo);
        LocalDate startDate = LocalDate.now().minusDays(daysAgo + pass.getValidityDays());
        LocalDate endDate = startDate.plusDays(pass.getValidityDays());
        Membership ms = Membership.builder()
                .publicId(UUID.randomUUID().toString().replace("-", ""))
                .member(m)
                .totalCount(pass.getTotalCount() != null ? pass.getTotalCount() : 1)
                .remainingCount(0)
                .unlimited(pass.isUnlimited())
                .startDate(startDate)
                .endDate(endDate)
                .price(pass.getPrice())
                .status(MembershipStatus.EXPIRED)
                .membershipPass(pass)
                .build();
        membershipRepository.save(ms);

        Payment payment = Payment.builder()
                .orderId("demo_old_" + UUID.randomUUID().toString().substring(0, 8))
                .member(m)
                .membershipPass(pass)
                .amount(pass.getPrice())
                .method(methods[random.nextInt(methods.length)])
                .status(PaymentStatus.PENDING)
                .build();
        payment.confirm("mock_pk_old_" + UUID.randomUUID().toString().substring(0, 8),
                "카드", startDate.atTime(11, 0));
        payment.linkMembership(ms);
        paymentRepository.save(payment);
    }

    private void createRenewalMembership(Member m, MembershipPass pass,
                                          PaymentMethod[] methods, LocalDate previousEndDate) {
        // 이전 정기권 만료 직후 갱신
        LocalDate startDate = previousEndDate.plusDays(random.nextInt(7));
        LocalDate endDate = startDate.plusDays(pass.getValidityDays());
        // 이전 갱신 이력 (만료 상태)
        Membership ms = Membership.builder()
                .publicId(UUID.randomUUID().toString().replace("-", ""))
                .member(m)
                .totalCount(pass.getTotalCount() != null ? pass.getTotalCount() : 30)
                .remainingCount(0)
                .unlimited(pass.isUnlimited())
                .startDate(startDate)
                .endDate(endDate)
                .price(pass.getPrice())
                .status(MembershipStatus.EXPIRED)
                .membershipPass(pass)
                .build();
        membershipRepository.save(ms);

        Payment payment = Payment.builder()
                .orderId("demo_renew_" + UUID.randomUUID().toString().substring(0, 8))
                .member(m)
                .membershipPass(pass)
                .amount(pass.getPrice())
                .method(methods[random.nextInt(methods.length)])
                .status(PaymentStatus.PENDING)
                .build();
        payment.confirm("mock_pk_renew_" + UUID.randomUUID().toString().substring(0, 8),
                "카드", startDate.atTime(14, 30));
        payment.linkMembership(ms);
        paymentRepository.save(payment);
    }

    private void createReservationsAndAttendances(List<Member> members, List<Membership> memberships,
                                                   List<ClassSchedule> schedules) {
        List<Member> activeMembers = members.stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .toList();
        Map<Long, Membership> membershipMap = new HashMap<>();
        for (Membership ms : memberships) {
            if (ms.getStatus() == MembershipStatus.ACTIVE) {
                membershipMap.putIfAbsent(ms.getMember().getId(), ms);
            }
        }

        // 회원별 출석 성향(0.5 ~ 1.0): 일부는 노쇼 다발, 일부는 모범생
        Map<Long, Double> attendanceTendency = new HashMap<>();
        for (Member m : activeMembers) {
            attendanceTendency.put(m.getId(), 0.55 + random.nextDouble() * 0.45);
        }

        int resCount = 0;
        for (ClassSchedule cs : schedules) {
            if (cs.getStatus() == ClassScheduleStatus.CANCELLED) continue;

            // 일자별 인기도: 미래 0~3일 = 100%, 향후 1주 = 80%, 그 이후 60%, 과거 = 70~85%
            double fillRate;
            long daysFromNow = LocalDate.now().until(cs.getClassDate()).getDays();
            if (daysFromNow < -7) fillRate = 0.7 + random.nextDouble() * 0.2;     // 1주+ 과거
            else if (daysFromNow < 0) fillRate = 0.75 + random.nextDouble() * 0.2; // 1주 내 과거
            else if (daysFromNow == 0) fillRate = 0.85 + random.nextDouble() * 0.15; // 오늘
            else if (daysFromNow <= 3) fillRate = 0.85 + random.nextDouble() * 0.15; // 3일 내
            else if (daysFromNow <= 7) fillRate = 0.7 + random.nextDouble() * 0.2;  // 1주 내
            else fillRate = 0.4 + random.nextDouble() * 0.3;                         // 1주+ 미래

            // 시간대 가중 (저녁 19시 = 인기, 점심 = 보통)
            int hour = cs.getStartTime().getHour();
            if (hour == 19 || hour == 18 || hour == 10) fillRate = Math.min(1.0, fillRate * 1.15);
            if (hour == 7 || hour == 12) fillRate *= 0.85;

            int slots = Math.max(1, (int) (cs.getMaxCapacity() * fillRate));
            slots = Math.min(slots, cs.getMaxCapacity());

            Set<Long> booked = new HashSet<>();

            for (int s = 0; s < slots && booked.size() < activeMembers.size(); s++) {
                Member m = activeMembers.get((resCount + s + cs.getStartTime().getHour()) % activeMembers.size());
                if (booked.contains(m.getId())) continue;
                Membership ms = membershipMap.get(m.getId());
                if (ms == null) continue;

                booked.add(m.getId());

                ReservationStatus status = ReservationStatus.CONFIRMED;

                // 과거 수업: 일부 취소/노쇼
                if (cs.getClassDate().isBefore(LocalDate.now())) {
                    int roll = random.nextInt(100);
                    double tendency = attendanceTendency.getOrDefault(m.getId(), 0.85);
                    int noShowCutoff = (int) ((1 - tendency) * 50); // 0~22
                    if (roll < 4) status = ReservationStatus.CANCELLED;
                    else if (roll < 4 + noShowCutoff) status = ReservationStatus.NO_SHOW;
                }

                Reservation res = Reservation.builder()
                        .member(m)
                        .classSchedule(cs)
                        .membership(ms)
                        .status(status)
                        .build();
                Reservation saved = reservationRepository.save(res);

                // 출석 기록
                if (cs.getClassDate().isBefore(LocalDate.now()) && saved.getStatus() == ReservationStatus.CONFIRMED) {
                    AttendanceStatus attStatus;
                    int attRoll = random.nextInt(100);
                    double tendency = attendanceTendency.getOrDefault(m.getId(), 0.85);
                    int attendedCutoff = (int) (tendency * 100);  // 55~100
                    int lateCutoff = attendedCutoff + 5;
                    int absentCutoff = lateCutoff + 3;
                    if (attRoll < attendedCutoff - 5) attStatus = AttendanceStatus.ATTENDED;
                    else if (attRoll < lateCutoff) attStatus = AttendanceStatus.LATE;
                    else if (attRoll < absentCutoff) attStatus = AttendanceStatus.ABSENT;
                    else attStatus = AttendanceStatus.NO_SHOW;

                    Attendance att = Attendance.builder()
                            .reservation(saved)
                            .member(m)
                            .classSchedule(cs)
                            .status(attStatus)
                            .build();
                    attendanceRepository.save(att);
                } else if (!cs.getClassDate().isBefore(LocalDate.now())) {
                    Attendance att = Attendance.createPending(saved);
                    attendanceRepository.save(att);
                }

                if (saved.getStatus() == ReservationStatus.CONFIRMED) {
                    try { cs.incrementCount(); } catch (Exception ignored) {}
                }
                resCount++;
            }
        }
        log.info("예약 {}건, 출석 {}건 생성", reservationRepository.count(), attendanceRepository.count());
    }

    /**
     * 수업 유형 기본 4종을 자바 코드로 생성한다.
     * portfolio 등 R__dev_seed.sql이 적용되지 않는 환경에서 사용.
     */
    private List<LessonType> createDefaultLessonTypes() {
        List<LessonType> result = new ArrayList<>();
        result.add(lessonTypeRepository.save(LessonType.builder()
                .name("개인").maxCapacity(1).durationMinutes(50).deductionCount(2).active(true).build()));
        result.add(lessonTypeRepository.save(LessonType.builder()
                .name("듀엣").maxCapacity(2).durationMinutes(50).deductionCount(1).active(true).build()));
        result.add(lessonTypeRepository.save(LessonType.builder()
                .name("그룹").maxCapacity(8).durationMinutes(50).deductionCount(1).active(true).build()));
        result.add(lessonTypeRepository.save(LessonType.builder()
                .name("체험").maxCapacity(1).durationMinutes(50).deductionCount(1).active(true).build()));
        return result;
    }

    /**
     * 정기권 종류 기본 4종을 자바 코드로 생성한다.
     */
    private List<MembershipPass> createDefaultMembershipPasses() {
        List<MembershipPass> result = new ArrayList<>();
        result.add(membershipPassRepository.save(MembershipPass.builder()
                .publicId("pass_8_group").name("8회권").price(new BigDecimal("180000"))
                .totalCount(8).validityDays(60).unlimited(false).displayOrder(1).build()));
        result.add(membershipPassRepository.save(MembershipPass.builder()
                .publicId("pass_12_group").name("12회권").price(new BigDecimal("250000"))
                .totalCount(12).validityDays(90).unlimited(false).displayOrder(2).build()));
        result.add(membershipPassRepository.save(MembershipPass.builder()
                .publicId("pass_unlimited").name("무제한권").price(new BigDecimal("350000"))
                .totalCount(null).validityDays(30).unlimited(true).monthlyLimit(30).displayOrder(3).build()));
        result.add(membershipPassRepository.save(MembershipPass.builder()
                .publicId("pass_10_private").name("개인 10회권").price(new BigDecimal("500000"))
                .totalCount(10).validityDays(90).unlimited(false).displayOrder(4).build()));
        return result;
    }
}
