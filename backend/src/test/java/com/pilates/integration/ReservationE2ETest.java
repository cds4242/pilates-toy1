package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
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

    @BeforeEach
    void clearRedis() {
        Set<String> keys = redisTemplate.keys("sms:*");
        if (keys != null) redisTemplate.delete(keys);
        Set<String> authKeys = redisTemplate.keys("auth:*");
        if (authKeys != null) redisTemplate.delete(authKeys);
    }

    private String[] signup(String phone) throws Exception {
        mockMvc.perform(post("/api/auth/sms/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phone + "\"}")).andExpect(status().isOk());
        String code = redisTemplate.opsForValue().get("sms:code:" + phone);
        MvcResult vr = mockMvc.perform(post("/api/auth/sms/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"" + code + "\"}")).andReturn();
        String vtoken = objectMapper.readTree(vr.getResponse().getContentAsString()).get("data").get("verifiedToken").asText();
        MvcResult sr = mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"verifiedToken\":\"" + vtoken + "\",\"name\":\"ResUser\",\"password\":\"Test1234!\",\"gender\":\"MALE\"}")).andReturn();
        JsonNode data = objectMapper.readTree(sr.getResponse().getContentAsString()).get("data");
        String token = data.get("accessToken").asText();
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        String memberId = objectMapper.readTree(payload).get("sub").asText();
        return new String[]{token, memberId};
    }

    /** 강사+수업유형+정기권종류+정기권발급+수업 생성 → classScheduleId 반환 */
    private long[] setupFullScenario(String token, String memberId, String suffix, int capacity) throws Exception {
        // 강사
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사" + suffix + "\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 유형
        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형" + suffix + "\",\"maxCapacity\":" + capacity + ",\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 종류
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES패스" + suffix + "\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 발급
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // 수업 (미래 날짜)
        LocalDate futureDate = LocalDate.now().plusDays(7);
        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
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
        String[] auth = signup("01044440001");
        String token = auth[0]; String memberId = auth[1];
        long[] setup = setupFullScenario(token, memberId, "S1", 8);
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
        String[] auth = signup("01044440002");
        String token = auth[0]; String memberId = auth[1];
        long[] setup = setupFullScenario(token, memberId, "S2", 8);

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
        String[] auth1 = signup("01044440031");
        String token1 = auth1[0]; String memberId1 = auth1[1];
        long[] setup = setupFullScenario(token1, memberId1, "S3", 1); // 정원 1

        // 첫 회원 예약
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + setup[0] + "}")).andExpect(status().isOk());

        // 두 번째 회원 가입 + 정기권
        String[] auth2 = signup("01044440032");
        String token2 = auth2[0]; String memberId2 = auth2[1];
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + token2).contentType(MediaType.APPLICATION_JSON)
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
        String[] auth = signup("01044440004");
        String token = auth[0]; String memberId = auth[1];

        // 강사+수업유형+수업만 생성 (정기권 없음)
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES강사S4\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();
        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"RES유형S4\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();
        LocalDate futureDate = LocalDate.now().plusDays(7);
        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
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
        // 회원 2명 가입
        String[] auth1 = signup("01044440051");
        String token1 = auth1[0]; String memberId1 = auth1[1];
        String[] auth2 = signup("01044440052");
        String token2 = auth2[0]; String memberId2 = auth2[1];

        // 수업 세팅 (정원 1)
        long[] setup = setupFullScenario(token1, memberId1, "S5", 1);
        Long classId = setup[0];

        // 두 번째 회원에게도 정기권
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + token2).contentType(MediaType.APPLICATION_JSON)
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
}
