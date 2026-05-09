package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.domain.reservation.scheduler.NoShowMarkingScheduler;
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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 예약 도메인 E2E 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private NoShowMarkingScheduler noShowMarkingScheduler;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRepository adminRepository;
    @Autowired private InstructorRepository instructorRepository;

    private AuthTestHelper authHelper;

    @BeforeEach
    void clearRedis() {
        authHelper = new AuthTestHelper(mockMvc, objectMapper, redisTemplate, passwordEncoder, adminRepository, instructorRepository);
        Set<String> keys = redisTemplate.keys("sms:*");
        if (keys != null) redisTemplate.delete(keys);
        Set<String> authKeys = redisTemplate.keys("auth:*");
        if (authKeys != null) redisTemplate.delete(authKeys);
    }

    /** 강사+수업유형+정기권종류+정기권발급+수업 생성 → classScheduleId 반환 */
    private long[] setupFullScenario(String adminToken, String memberToken, String memberId, String suffix, int capacity) throws Exception {
        // 강사
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사" + suffix + "\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 유형
        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형" + suffix + "\",\"maxCapacity\":" + capacity + ",\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 종류
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스" + suffix + "\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 발급
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // 수업 (미래 날짜)
        LocalDate futureDate = LocalDate.now().plusDays(7);
        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"10:00\",\"endTime\":\"10:50\",\"maxCapacity\":" + capacity + "}")).andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(cs.getResponse().getContentAsString()).get("data").get("id").asLong();

        return new long[]{classId, ltId, passId};
    }

    // ═══════════════════════════════════════════
    // 시나리오 1: 정상 예약 + 취소
    // ═══════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("시나리오1: 예약 → 잔여 차감 → 취소 → 잔여 복구")
    void scenario1_reserveAndCancel() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01044440001");
        String token = auth[0]; String memberId = auth[1];
        long[] setup = setupFullScenario(adminToken, token, memberId, "S1", 8);
        Long classId = setup[0];

        // 예약
        MvcResult resResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classScheduleId\":" + classId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andReturn();
        Long resId = objectMapper.readTree(resResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 잔여 차감 확인
        MvcResult mship = mockMvc.perform(get("/api/members/me/memberships")
                .header("Authorization", "Bearer " + token)).andReturn();
        int remaining = objectMapper.readTree(mship.getResponse().getContentAsString())
                .get("data").get(0).get("remainingCount").asInt();
        assertThat(remaining).isEqualTo(9); // 10 - 1

        // 취소
        mockMvc.perform(delete("/api/reservations/" + resId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 잔여 복구 확인
        MvcResult mship2 = mockMvc.perform(get("/api/members/me/memberships")
                .header("Authorization", "Bearer " + token)).andReturn();
        int restored = objectMapper.readTree(mship2.getResponse().getContentAsString())
                .get("data").get(0).get("remainingCount").asInt();
        assertThat(restored).isEqualTo(10);
    }

    // ═══════════════════════════════════════════
    // 시나리오 2: 중복 예약 방지
    // ═══════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("시나리오2: 같은 수업 두 번 예약 → RES_002")
    void scenario2_duplicateReservation() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01044440002");
        String token = auth[0]; String memberId = auth[1];
        long[] setup = setupFullScenario(adminToken, token, memberId, "S2", 8);

        // 첫 예약
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + setup[0] + "}")).andExpect(status().isOk());

        // 두 번째 예약 → 거부
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classScheduleId\":" + setup[0] + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RES_002"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 3: 정원 초과
    // ═══════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("시나리오3: 정원 1명 수업에 2번째 예약 → RES_004")
    void scenario3_capacityExceeded() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth1 = authHelper.loginAsMember("01044440031");
        String token1 = auth1[0]; String memberId1 = auth1[1];
        long[] setup = setupFullScenario(adminToken, token1, memberId1, "S3", 1); // 정원 1

        // 첫 회원 예약
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + setup[0] + "}")).andExpect(status().isOk());

        // 두 번째 회원 가입 + 정기권
        String[] auth2 = authHelper.loginAsMember("01044440032");
        String token2 = auth2[0]; String memberId2 = auth2[1];
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId2 + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + setup[1] + "],\"membershipPassId\":" + setup[2] + "}")).andExpect(status().isOk());

        // 두 번째 회원 예약 → 정원 초과
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token2).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classScheduleId\":" + setup[0] + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RES_004"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 4: 정기권 없으면 예약 불가
    // ═══════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("시나리오4: 정기권 없는 회원 예약 → RES_003")
    void scenario4_noMembership() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01044440004");
        String token = auth[0]; String memberId = auth[1];

        // 강사+수업유형+수업만 생성 (정기권 없음)
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S4\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();
        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S4\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();
        LocalDate futureDate = LocalDate.now().plusDays(7);
        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"14:00\",\"endTime\":\"14:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(cs.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 예약 시도 → 정기권 없음
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classScheduleId\":" + classId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RES_003"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 5: 동시 예약 (정원 1, 2명 동시)
    // ═══════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("시나리오5: 정원 1석에 2명 동시 예약 → 1명만 성공")
    void scenario5_concurrentReservation() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        // 회원 2명 가입
        String[] auth1 = authHelper.loginAsMember("01044440051");
        String token1 = auth1[0]; String memberId1 = auth1[1];
        String[] auth2 = authHelper.loginAsMember("01044440052");
        String token2 = auth2[0]; String memberId2 = auth2[1];

        // 수업 세팅 (정원 1)
        long[] setup = setupFullScenario(adminToken, token1, memberId1, "S5", 1);
        Long classId = setup[0];

        // 두 번째 회원에게도 정기권
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId2 + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + setup[1] + "],\"membershipPassId\":" + setup[2] + "}")).andExpect(status().isOk());

        // 동시 호출
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (String[] authPair : new String[][]{auth1, auth2}) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await(); // 동시 시작
                    MvcResult r = mockMvc.perform(post("/api/reservations")
                            .header("Authorization", "Bearer " + authPair[0])
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"classScheduleId\":" + classId + "}")).andReturn();
                    if (r.getResponse().getStatus() == 200) successCount.incrementAndGet();
                    else failCount.incrementAndGet();
                } catch (Exception e) { failCount.incrementAndGet(); }
            });
        }

        ready.await();
        start.countDown(); // 동시 시작!
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }

    // ═══════════════════════════════════════════
    // 시나리오 6: 취소 가능 시간 만료 (수업 시작 2시간 이내)
    // ═══════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("시나리오6: 수업 시작 2시간 이내 취소 → RES_006")
    void scenario6_cancelWindowExpired() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01044440006");
        String token = auth[0]; String memberId = auth[1];

        // 강사 + 수업유형 생성
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S6\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S6\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 종류 + 발급
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스S6\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // 수업: 오늘, 현재시간+1시간 (취소 불가 시간대)
        LocalDate today = LocalDate.now();
        LocalTime startTime = LocalTime.now().plusHours(1).withSecond(0).withNano(0);
        LocalTime endTime = startTime.plusMinutes(50);
        String startStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        String endStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + today + "\",\"startTime\":\"" + startStr + "\",\"endTime\":\"" + endStr + "\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(cs.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 예약
        MvcResult resResult = mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classId + "}")).andExpect(status().isOk()).andReturn();
        Long resId = objectMapper.readTree(resResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 취소 시도 → 시간 만료
        mockMvc.perform(delete("/api/reservations/" + resId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RES_006"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 7: 시간 겹침 예약 방지
    // ═══════════════════════════════════════════

    @Test
    @Order(7)
    @DisplayName("시나리오7: 동일 시간대 다른 수업 예약 → RES_010, 겹치지 않는 시간 → 성공")
    void scenario7_timeOverlap() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01044440007");
        String token = auth[0]; String memberId = auth[1];

        // 강사 + 수업유형
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S7\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S7\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스S7\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        LocalDate futureDate = LocalDate.now().plusDays(8);

        // 수업 A: 10:00-10:50
        MvcResult csA = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"10:00\",\"endTime\":\"10:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classAId = objectMapper.readTree(csA.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 강사2 (시간 충돌 방지를 위해 다른 강사)
        MvcResult ir2 = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S7B\",\"phone\":\"010-0000-0001\"}")).andExpect(status().isOk()).andReturn();
        Long instrId2 = objectMapper.readTree(ir2.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 B: 10:30-11:20 (A와 겹침)
        MvcResult csB = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId2 + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"10:30\",\"endTime\":\"11:20\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classBId = objectMapper.readTree(csB.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 강사3
        MvcResult ir3 = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S7C\",\"phone\":\"010-0000-0002\"}")).andExpect(status().isOk()).andReturn();
        Long instrId3 = objectMapper.readTree(ir3.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 C: 11:00-11:50 (A와 겹치지 않음)
        MvcResult csC = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId3 + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"11:00\",\"endTime\":\"11:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classCId = objectMapper.readTree(csC.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 A 예약 → 성공
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classAId + "}")).andExpect(status().isOk());

        // 수업 B 예약 → 시간 겹침 RES_010
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classScheduleId\":" + classBId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RES_010"));

        // 수업 C 예약 → 성공 (겹치지 않음)
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classCId + "}")).andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════
    // 시나리오 8: 동시 정기권 차감 (비관적 락)
    // ═══════════════════════════════════════════

    @Test
    @Order(8)
    @org.junit.jupiter.api.Disabled("H2 환경에서 비관적 락 미지원 → MySQL 통합 테스트에서 검증")
    @DisplayName("시나리오8: 잔여 1회 정기권에 2개 수업 동시 예약 → 1건만 성공")
    void scenario8_concurrentMembershipDeduction() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01044440008");
        String token = auth[0]; String memberId = auth[1];

        // 강사 + 수업유형
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S8\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S8\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 (잔여 1회만)
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스S8\",\"price\":100000,\"totalCount\":1,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":1,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        LocalDate futureDate = LocalDate.now().plusDays(9);

        // 수업 2개 (다른 시간대)
        MvcResult cs1 = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"09:00\",\"endTime\":\"09:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId1 = objectMapper.readTree(cs1.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult cs2 = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"14:00\",\"endTime\":\"14:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId2 = objectMapper.readTree(cs2.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 동시 호출
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Long[] classIds = {classId1, classId2};
        for (Long cid : classIds) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    MvcResult r = mockMvc.perform(post("/api/reservations")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"classScheduleId\":" + cid + "}")).andReturn();
                    if (r.getResponse().getStatus() == 200) successCount.incrementAndGet();
                    else failCount.incrementAndGet();
                } catch (Exception e) { failCount.incrementAndGet(); }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }

    // ═══════════════════════════════════════════
    // 시나리오 9: 휴강 → 예약 자동 취소 + 정기권 복구
    // ═══════════════════════════════════════════

    @Test
    @Order(9)
    @DisplayName("시나리오9: 휴강 처리 → 3명 예약 자동 취소 + 정기권 복구")
    void scenario9_classCancelRestoresMemberships() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        // 3명 회원 가입
        String[] auth1 = authHelper.loginAsMember("01044440091");
        String token1 = auth1[0]; String memberId1 = auth1[1];
        String[] auth2 = authHelper.loginAsMember("01044440092");
        String token2 = auth2[0]; String memberId2 = auth2[1];
        String[] auth3 = authHelper.loginAsMember("01044440093");
        String token3 = auth3[0]; String memberId3 = auth3[1];

        // 강사 + 수업유형 (adminToken으로 admin 작업)
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S9\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S9\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 종류
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스S9\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 3명 모두에게 정기권 발급
        for (String mid : new String[]{memberId1, memberId2, memberId3}) {
            mockMvc.perform(post("/api/admin/memberships")
                    .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"memberId\":" + mid + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());
        }

        // 수업 생성
        LocalDate futureDate = LocalDate.now().plusDays(10);
        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"10:00\",\"endTime\":\"10:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(cs.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 3명 예약
        for (String t : new String[]{token1, token2, token3}) {
            mockMvc.perform(post("/api/reservations")
                    .header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"classScheduleId\":" + classId + "}")).andExpect(status().isOk());
        }

        // 잔여 차감 확인 (각 9회)
        for (String t : new String[]{token1, token2, token3}) {
            MvcResult mship = mockMvc.perform(get("/api/members/me/memberships")
                    .header("Authorization", "Bearer " + t)).andReturn();
            int remaining = objectMapper.readTree(mship.getResponse().getContentAsString())
                    .get("data").get(0).get("remainingCount").asInt();
            assertThat(remaining).isEqualTo(9);
        }

        // 휴강 처리
        mockMvc.perform(post("/api/admin/class-schedules/" + classId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 수업 상태 CANCELLED 확인
        MvcResult csDetail = mockMvc.perform(get("/api/admin/class-schedules/" + classId)
                .header("Authorization", "Bearer " + adminToken)).andReturn();
        String csStatus = objectMapper.readTree(csDetail.getResponse().getContentAsString())
                .get("data").get("status").asText();
        assertThat(csStatus).isEqualTo("CANCELLED");

        // 정기권 복구 확인 (각 10회로 복구)
        for (String t : new String[]{token1, token2, token3}) {
            MvcResult mship = mockMvc.perform(get("/api/members/me/memberships")
                    .header("Authorization", "Bearer " + t)).andReturn();
            int remaining = objectMapper.readTree(mship.getResponse().getContentAsString())
                    .get("data").get(0).get("remainingCount").asInt();
            assertThat(remaining).isEqualTo(10);
        }

        // 각 회원의 예약 목록에서 해당 예약이 CANCELLED인지 확인
        for (String t : new String[]{token1, token2, token3}) {
            MvcResult myRes = mockMvc.perform(get("/api/members/me/reservations")
                    .header("Authorization", "Bearer " + t)).andReturn();
            JsonNode reservations = objectMapper.readTree(myRes.getResponse().getContentAsString()).get("data");
            boolean foundCancelled = false;
            for (JsonNode res : reservations) {
                if (res.get("classScheduleId").asLong() == classId) {
                    assertThat(res.get("status").asText()).isEqualTo("CANCELLED");
                    foundCancelled = true;
                }
            }
            assertThat(foundCancelled).isTrue();
        }
    }

    // ═══════════════════════════════════════════
    // 시나리오 10: 회원 시간표 조회 시 myReservationStatus
    // ═══════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("시나리오10: 회원 시간표 조회 → 예약 수업 RESERVED, 비예약 NOT_RESERVED")
    void scenario10_myReservationStatus() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01044440010");
        String token = auth[0]; String memberId = auth[1];

        // 강사 + 수업유형
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S10\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S10\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스S10\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        LocalDate futureDate = LocalDate.now().plusDays(11);

        // 수업 2개 생성
        MvcResult csA = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"10:00\",\"endTime\":\"10:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classAId = objectMapper.readTree(csA.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult csB = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"14:00\",\"endTime\":\"14:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classBId = objectMapper.readTree(csB.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 A만 예약
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classAId + "}")).andExpect(status().isOk());

        // 회원용 시간표 조회
        MvcResult listResult = mockMvc.perform(get("/api/class-schedules")
                        .header("Authorization", "Bearer " + token)
                        .param("from", futureDate.toString())
                        .param("to", futureDate.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode classes = objectMapper.readTree(listResult.getResponse().getContentAsString()).get("data");
        for (JsonNode cls : classes) {
            long cid = cls.get("id").asLong();
            if (cid == classAId) {
                assertThat(cls.get("myReservationStatus").asText()).isEqualTo("RESERVED");
            } else if (cid == classBId) {
                assertThat(cls.get("myReservationStatus").asText()).isEqualTo("NOT_RESERVED");
            }
        }
    }

    // ═══════════════════════════════════════════
    // 시나리오 11: 강사 수업 상세에 예약자 리스트
    // ═══════════════════════════════════════════

    @Test
    @Order(11)
    @DisplayName("시나리오11: 강사 수업 상세 → 예약자 2명 리스트 확인")
    void scenario11_instructorClassDetailWithReservations() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        // 2명 회원 가입
        String[] auth1 = authHelper.loginAsMember("01044440111");
        String token1 = auth1[0]; String memberId1 = auth1[1];
        String[] auth2 = authHelper.loginAsMember("01044440112");
        String token2 = auth2[0]; String memberId2 = auth2[1];

        // 강사 + 수업유형
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S11\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S11\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스S11\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 2명 모두 정기권 발급
        for (String mid : new String[]{memberId1, memberId2}) {
            mockMvc.perform(post("/api/admin/memberships")
                    .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"memberId\":" + mid + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());
        }

        // 수업 생성
        LocalDate futureDate = LocalDate.now().plusDays(12);
        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"10:00\",\"endTime\":\"10:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(cs.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 2명 예약
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classId + "}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token2).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classId + "}")).andExpect(status().isOk());

        // admin 수업 상세에서 currentCount = 2
        MvcResult detail = mockMvc.perform(get("/api/admin/class-schedules/" + classId)
                .header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();
        JsonNode detailData = objectMapper.readTree(detail.getResponse().getContentAsString()).get("data");
        assertThat(detailData.get("currentCount").asInt()).isEqualTo(2);

        // 또한 각 회원의 예약 목록에서 해당 수업 예약이 CONFIRMED인지 확인
        for (String t : new String[]{token1, token2}) {
            MvcResult myRes = mockMvc.perform(get("/api/members/me/reservations")
                    .header("Authorization", "Bearer " + t)).andReturn();
            JsonNode reservations = objectMapper.readTree(myRes.getResponse().getContentAsString()).get("data");
            boolean found = false;
            for (JsonNode res : reservations) {
                if (res.get("classScheduleId").asLong() == classId) {
                    assertThat(res.get("status").asText()).isEqualTo("CONFIRMED");
                    found = true;
                }
            }
            assertThat(found).isTrue();
        }
    }

    // ═══════════════════════════════════════════
    // 시나리오 12: NoShow 스케줄러
    // ═══════════════════════════════════════════

    @Test
    @Order(12)
    @DisplayName("시나리오12: 종료된 수업의 CONFIRMED 예약 → NoShow 스케줄러 → NO_SHOW 전환")
    void scenario12_noShowScheduler() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01044440012");
        String token = auth[0]; String memberId = auth[1];

        // 강사 + 수업유형
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S12\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S12\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스S12\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // 과거 수업 (오늘, 현재 시간 - 2시간 시작, -1시간10분 종료 → 종료 후 50분 이상 경과)
        LocalDate today = LocalDate.now();
        LocalTime endTime = LocalTime.now().minusMinutes(31).withSecond(0).withNano(0);
        LocalTime startTime = endTime.minusMinutes(50);

        // 시간이 0시 이전이 되면 스킵 (자정 근처 테스트 방지)
        if (startTime.isAfter(endTime)) {
            return; // 자정 근처 → 테스트 스킵
        }

        String startStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        String endStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + today + "\",\"startTime\":\"" + startStr + "\",\"endTime\":\"" + endStr + "\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(cs.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 예약 (과거 수업이지만 당일이므로 예약 가능 — ReservationService는 classDate >= today로 판단)
        MvcResult resResult = mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classId + "}")).andExpect(status().isOk()).andReturn();
        Long resId = objectMapper.readTree(resResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // NoShow 스케줄러 호출
        noShowMarkingScheduler.markOverdueReservations();

        // 예약 상태 확인 → NO_SHOW
        MvcResult myRes = mockMvc.perform(get("/api/members/me/reservations")
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode reservations = objectMapper.readTree(myRes.getResponse().getContentAsString()).get("data");
        boolean foundNoShow = false;
        for (JsonNode res : reservations) {
            if (res.get("id") != null && res.get("classScheduleId").asLong() == classId) {
                assertThat(res.get("status").asText()).isEqualTo("NO_SHOW");
                foundNoShow = true;
            }
        }
        assertThat(foundNoShow).isTrue();
    }

    // ═══════════════════════════════════════════
    // 시나리오 13: 시간 겹침 에러 코드 정확성 검증
    // ═══════════════════════════════════════════

    @Test
    @Order(13)
    @DisplayName("시나리오13: 동일 시간 다른 수업 겹침 → RES_010 에러 코드 + 메시지 확인")
    void scenario13_timeOverlapErrorCodeVerification() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01044440013");
        String token = auth[0]; String memberId = auth[1];

        // 강사 + 수업유형
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S13\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S13\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스S13\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        LocalDate futureDate = LocalDate.now().plusDays(13);

        // 강사 2명 (같은 시간대 다른 수업을 위해)
        MvcResult ir2 = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S13B\",\"phone\":\"010-0000-0001\"}")).andExpect(status().isOk()).andReturn();
        Long instrId2 = objectMapper.readTree(ir2.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 A: 15:00-15:50
        MvcResult csA = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"15:00\",\"endTime\":\"15:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classAId = objectMapper.readTree(csA.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 B: 15:00-15:50 (완전 동일 시간, 다른 강사)
        MvcResult csB = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId2 + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"15:00\",\"endTime\":\"15:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classBId = objectMapper.readTree(csB.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 A 예약
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classAId + "}")).andExpect(status().isOk());

        // 수업 B 예약 → 완전히 겹치는 시간 → RES_010 + HTTP 409 Conflict
        MvcResult errorResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classScheduleId\":" + classBId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RES_010"))
                .andReturn();

        // 에러 메시지도 포함되어 있는지 확인
        JsonNode error = objectMapper.readTree(errorResult.getResponse().getContentAsString()).get("error");
        assertThat(error.get("code").asText()).isEqualTo("RES_010");
        assertThat(error.get("message").asText()).isNotBlank();
    }
}
