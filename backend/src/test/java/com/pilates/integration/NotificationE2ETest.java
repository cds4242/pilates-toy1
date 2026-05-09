package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilates.common.notification.kakao.MockKakaoAlimtalkClient;
import com.pilates.common.sms.MockSmsService;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipStatus;
import com.pilates.domain.membership.repository.MembershipRepository;
import com.pilates.domain.notification.entity.Notification;
import com.pilates.domain.notification.entity.NotificationStatus;
import com.pilates.domain.notification.entity.NotificationType;
import com.pilates.domain.notification.repository.NotificationRepository;
import com.pilates.domain.notification.scheduler.MembershipExpirationReminderScheduler;
import com.pilates.domain.notification.scheduler.ReservationReminderScheduler;
import com.pilates.integration.support.AuthTestHelper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 알림 도메인 E2E 통합 테스트.
 * 10개 시나리오로 알림 발송, 폴백, 스케줄러, 권한 분리를 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRepository adminRepository;
    @Autowired private InstructorRepository instructorRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private MockKakaoAlimtalkClient mockAlimtalkClient;
    @Autowired private MockSmsService mockSmsService;
    @Autowired private ReservationReminderScheduler reminderScheduler;
    @Autowired private MembershipExpirationReminderScheduler expirationScheduler;

    private AuthTestHelper authHelper;

    @BeforeEach
    void setUp() {
        authHelper = new AuthTestHelper(mockMvc, objectMapper, redisTemplate, passwordEncoder, adminRepository, instructorRepository);
        Set<String> keys = redisTemplate.keys("sms:*");
        if (keys != null) redisTemplate.delete(keys);
        Set<String> authKeys = redisTemplate.keys("auth:*");
        if (authKeys != null) redisTemplate.delete(authKeys);
        mockAlimtalkClient.resetSendCallCount();
        mockSmsService.resetSendCallCount();
        mockSmsService.setForceFailMode(false);
    }

    /** 강사+수업유형+정기권+수업 → classScheduleId, lessonTypeId, passId 반환 */
    private long[] setupFullScenario(String adminToken, String memberId, String suffix, int capacity) throws Exception {
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI강사" + suffix + "\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI유형" + suffix + "\",\"maxCapacity\":" + capacity + ",\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI패스" + suffix + "\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        LocalDate futureDate = LocalDate.now().plusDays(7);
        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"10:00\",\"endTime\":\"10:50\",\"maxCapacity\":" + capacity + "}")).andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(cs.getResponse().getContentAsString()).get("data").get("id").asLong();

        return new long[]{classId, ltId, passId, instrId};
    }

    private void waitForAsync() throws InterruptedException {
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════
    // 시나리오 1: 예약 생성 → RESERVATION_CONFIRM 알림 발송
    // ═══════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("시나리오1: 예약 생성 → RESERVATION_CONFIRM 알림 발송 + notifications 테이블 INSERT")
    void scenario1_reservationConfirmNotification() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01088880001");
        String token = auth[0]; String memberId = auth[1];
        long[] setup = setupFullScenario(adminToken, memberId, "N1", 8);

        int beforeAlimtalk = mockAlimtalkClient.getSendCallCount();

        // 예약 생성
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + setup[0] + "}")).andExpect(status().isOk());

        waitForAsync();

        // Mock 알림톡 호출 확인 (RESERVATION_CONFIRM + NEW_RESERVATION = 2건)
        assertThat(mockAlimtalkClient.getSendCallCount() - beforeAlimtalk).isGreaterThanOrEqualTo(2);

        // notifications 테이블에 INSERT 확인
        List<Notification> notifications = notificationRepository.findAll().stream()
                .filter(n -> n.getMember().getId().equals(Long.valueOf(memberId)))
                .filter(n -> n.getType() == NotificationType.RESERVATION_CONFIRM)
                .toList();
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getStatus()).isIn(NotificationStatus.SENT, NotificationStatus.FALLBACK_SENT);
    }

    // ═══════════════════════════════════════════
    // 시나리오 2: 예약 생성 → 강사에게 NEW_RESERVATION 알림
    // ═══════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("시나리오2: 예약 생성 → 회원 RESERVATION_CONFIRM + 강사 NEW_RESERVATION 두 건 발송")
    void scenario2_newReservationToInstructor() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01088880002");
        String token = auth[0]; String memberId = auth[1];
        long[] setup = setupFullScenario(adminToken, memberId, "N2", 8);

        int beforeAlimtalk = mockAlimtalkClient.getSendCallCount();

        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + setup[0] + "}")).andExpect(status().isOk());

        waitForAsync();

        // 회원 + 강사 = 최소 2건 발송
        assertThat(mockAlimtalkClient.getSendCallCount() - beforeAlimtalk).isGreaterThanOrEqualTo(2);

        // NEW_RESERVATION 타입 알림 존재 확인
        List<Notification> instrNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getMember().getId().equals(Long.valueOf(memberId)))
                .filter(n -> n.getType() == NotificationType.NEW_RESERVATION)
                .toList();
        assertThat(instrNotifs).isNotEmpty();
    }

    // ═══════════════════════════════════════════
    // 시나리오 3: 예약 취소 → RESERVATION_CANCEL 알림
    // ═══════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("시나리오3: 예약 취소 → RESERVATION_CANCEL 알림 발송")
    void scenario3_reservationCancelNotification() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01088880003");
        String token = auth[0]; String memberId = auth[1];
        long[] setup = setupFullScenario(adminToken, memberId, "N3", 8);

        // 예약 생성
        MvcResult resResult = mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + setup[0] + "}")).andExpect(status().isOk()).andReturn();
        Long resId = objectMapper.readTree(resResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        waitForAsync();
        int beforeAlimtalk = mockAlimtalkClient.getSendCallCount();

        // 예약 취소
        mockMvc.perform(delete("/api/reservations/" + resId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        waitForAsync();

        // RESERVATION_CANCEL 알림 발송 확인
        assertThat(mockAlimtalkClient.getSendCallCount() - beforeAlimtalk).isGreaterThanOrEqualTo(1);

        List<Notification> cancelNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getMember().getId().equals(Long.valueOf(memberId)))
                .filter(n -> n.getType() == NotificationType.RESERVATION_CANCEL)
                .toList();
        assertThat(cancelNotifs).isNotEmpty();
    }

    // ═══════════════════════════════════════════
    // 시나리오 4: 알림톡 실패 → SMS 폴백 성공
    // ═══════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("시나리오4: 알림톡 실패 → SMS 폴백 성공 → status=FALLBACK_SENT")
    void scenario4_alimtalkFailSmsFallback() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01088880004");
        String token = auth[0]; String memberId = auth[1];

        // 강사 + 수업유형 + 정기권
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI강사N4\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI유형N4\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI패스N4\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // FAIL_ prefix 템플릿 코드 → 알림톡 실패 → SMS 폴백 검증
        Notification failNotif = Notification.create(
                getMemberById(Long.valueOf(memberId)),
                NotificationType.RESERVATION_CONFIRM,
                "FAIL_ALIMTALK_TEST",
                "[테스트] 알림톡 실패 폴백 테스트",
                java.time.LocalDateTime.now());
        failNotif = notificationRepository.save(failNotif);

        int beforeSms = mockSmsService.getSendCallCount();

        // sendSync 호출 (FAIL_ prefix → 알림톡 실패 → SMS 폴백)
        getNotificationService().sendSync(failNotif.getId());

        // SMS 폴백 발송됨
        assertThat(mockSmsService.getSendCallCount() - beforeSms).isGreaterThanOrEqualTo(1);

        // status = FALLBACK_SENT
        Notification updated = notificationRepository.findById(failNotif.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.FALLBACK_SENT);
    }

    // ═══════════════════════════════════════════
    // 시나리오 5: 알림톡 + SMS 모두 실패
    // ═══════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("시나리오5: 알림톡 + SMS 모두 실패 → status=FAILED + failure_reason 기록")
    void scenario5_allChannelsFailed() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01088880005");
        String memberId = auth[1];

        // SMS 강제 실패 모드
        mockSmsService.setForceFailMode(true);

        Notification failNotif = Notification.create(
                getMemberById(Long.valueOf(memberId)),
                NotificationType.RESERVATION_CONFIRM,
                "FAIL_ALL_TEST",
                "[테스트] 전체 실패 테스트",
                java.time.LocalDateTime.now());
        failNotif = notificationRepository.save(failNotif);

        getNotificationService().sendSync(failNotif.getId());

        // status = FAILED
        Notification updated = notificationRepository.findById(failNotif.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(updated.getFailureReason()).isNotBlank();

        mockSmsService.setForceFailMode(false);
    }

    // ═══════════════════════════════════════════
    // 시나리오 6: 1시간 전 리마인드 스케줄러
    // ═══════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("시나리오6: 1시간 후 시작 예약 → 리마인드 스케줄러 → REMINDER_1HOUR 발송")
    void scenario6_reminderScheduler() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01088880006");
        String token = auth[0]; String memberId = auth[1];

        // 강사 + 수업유형 + 정기권
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI강사N6\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI유형N6\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI패스N6\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // 오늘 수업 (현재시각 + 30~50분 후 시작) — 스케줄러의 1시간 윈도우에 들어가도록
        LocalDate today = LocalDate.now();
        LocalTime startTime = LocalTime.now().plusMinutes(40).withSecond(0).withNano(0);
        LocalTime endTime = startTime.plusMinutes(50);
        String startStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        String endStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        // 자정 근처 방어
        if (endTime.isBefore(startTime)) {
            return;
        }

        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + today + "\",\"startTime\":\"" + startStr + "\",\"endTime\":\"" + endStr + "\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(cs.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 예약
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classId + "}")).andExpect(status().isOk());

        waitForAsync();
        int beforeAlimtalk = mockAlimtalkClient.getSendCallCount();

        // 스케줄러 수동 호출
        reminderScheduler.sendReminders();

        waitForAsync();

        // REMINDER_1HOUR 알림 발송 확인
        List<Notification> reminders = notificationRepository.findAll().stream()
                .filter(n -> n.getMember().getId().equals(Long.valueOf(memberId)))
                .filter(n -> n.getType() == NotificationType.REMINDER_1HOUR)
                .toList();
        assertThat(reminders).isNotEmpty();
    }

    // ═══════════════════════════════════════════
    // 시나리오 7: 정기권 만료 3일 전 알림
    // ═══════════════════════════════════════════

    @Test
    @Order(7)
    @DisplayName("시나리오7: 3일 후 만료 정기권 → 스케줄러 → MEMBERSHIP_EXPIRING 발송")
    void scenario7_membershipExpirationReminder() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01088880007");
        String memberId = auth[1];

        // 강사 + 수업유형 + 정기권 (3일 후 만료되도록 validityDays 조절)
        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI유형N7\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"NOTI패스N7\",\"price\":100000,\"totalCount\":10,\"validityDays\":3,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":3,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // 정기권의 endDate를 3일 후로 직접 설정 (API로는 정확히 맞추기 어려움)
        // validityDays=3이면 startDate=today, endDate=today+3이므로 이미 3일 후 만료
        waitForAsync();

        // 스케줄러 호출
        expirationScheduler.sendExpirationReminders();

        waitForAsync();

        // MEMBERSHIP_EXPIRING 알림 발송 확인
        List<Notification> expiryNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getMember().getId().equals(Long.valueOf(memberId)))
                .filter(n -> n.getType() == NotificationType.MEMBERSHIP_EXPIRING)
                .toList();
        assertThat(expiryNotifs).isNotEmpty();
    }

    // ═══════════════════════════════════════════
    // 시나리오 8: 휴강 처리 시 알림 X
    // ═══════════════════════════════════════════

    @Test
    @Order(8)
    @DisplayName("시나리오8: 휴강 처리 시 알림 발송 X (의뢰인 정책)")
    void scenario8_classCancelNoNotification() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01088880008");
        String token = auth[0]; String memberId = auth[1];
        long[] setup = setupFullScenario(adminToken, memberId, "N8", 8);

        // 예약
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + setup[0] + "}")).andExpect(status().isOk());

        waitForAsync();

        // 알림톡 카운트 기록
        int beforeAlimtalk = mockAlimtalkClient.getSendCallCount();

        // 휴강 처리
        mockMvc.perform(post("/api/admin/class-schedules/" + setup[0] + "/cancel")
                .header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

        waitForAsync();

        // 휴강 시 알림톡 호출 카운트 0 (의뢰인 정책: 휴강 시 알림 X)
        assertThat(mockAlimtalkClient.getSendCallCount() - beforeAlimtalk).isEqualTo(0);
    }

    // ═══════════════════════════════════════════
    // 시나리오 9: 권한 분리
    // ═══════════════════════════════════════════

    @Test
    @Order(9)
    @DisplayName("시나리오9: 회원 → /api/admin/notifications 접근 시 403 + 다른 회원 알림 조회 403/404")
    void scenario9_accessControl() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth1 = authHelper.loginAsMember("01088880091");
        String token1 = auth1[0]; String memberId1 = auth1[1];
        String[] auth2 = authHelper.loginAsMember("01088880092");
        String memberId2 = auth2[1];

        // 회원 토큰으로 관리자 API → 403
        mockMvc.perform(get("/api/admin/notifications")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden());

        // 다른 회원의 알림 조회 시도 → 알림이 없으면 404, 있어도 403
        // 먼저 memberId2에 알림 생성
        Notification otherNotif = Notification.create(
                getMemberById(Long.valueOf(memberId2)),
                NotificationType.RESERVATION_CONFIRM,
                "TEST_ACCESS", "[테스트] 접근 제어", java.time.LocalDateTime.now());
        otherNotif = notificationRepository.save(otherNotif);

        // memberId1 토큰으로 memberId2의 알림 조회 → 403
        mockMvc.perform(get("/api/members/me/notifications/" + otherNotif.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════
    // 시나리오 10: 관리자 수동 재발송
    // ═══════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("시나리오10: FAILED 상태 알림 → 관리자 재발송 → status=SENT 변경")
    void scenario10_adminResend() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01088880010");
        String memberId = auth[1];

        // FAILED 상태 알림 시드
        Notification failedNotif = Notification.create(
                getMemberById(Long.valueOf(memberId)),
                NotificationType.RESERVATION_CONFIRM,
                "RESERVATION_CONFIRM",
                "[테스트] 재발송 테스트",
                java.time.LocalDateTime.now());
        failedNotif = notificationRepository.save(failedNotif);
        failedNotif.markAsFailed("초기 실패");
        notificationRepository.save(failedNotif);

        // 관리자 재발송
        mockMvc.perform(post("/api/admin/notifications/" + failedNotif.getId() + "/resend")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // status 변경 확인
        Notification updated = notificationRepository.findById(failedNotif.getId()).orElseThrow();
        assertThat(updated.getStatus()).isIn(NotificationStatus.SENT, NotificationStatus.FALLBACK_SENT);
    }

    // ── helper ──

    @Autowired
    private com.pilates.domain.member.repository.MemberRepository memberRepository;

    @Autowired
    private com.pilates.domain.notification.service.NotificationService notificationService;

    private com.pilates.domain.member.entity.Member getMemberById(Long id) {
        return memberRepository.findById(id).orElseThrow();
    }

    private com.pilates.domain.notification.service.NotificationService getNotificationService() {
        return notificationService;
    }
}
