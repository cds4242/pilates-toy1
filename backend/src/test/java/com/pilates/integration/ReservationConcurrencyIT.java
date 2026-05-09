package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예약 동시성 통합 테스트 (Testcontainers MySQL).
 * H2에서 비관적 락이 동작하지 않는 시나리오를 MySQL로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(
        named = "TESTCONTAINERS_ENABLED", matches = "true",
        disabledReason = "Testcontainers Docker 연결 필요. TESTCONTAINERS_ENABLED=true 환경변수로 활성화.")
class ReservationConcurrencyIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("pilates_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("app.encryption.key", () -> "dGVzdC1rZXktMzItYnl0ZXMtbG9uZy1leGFtcGxlISE=");
        registry.add("app.encryption.key-version", () -> "v1");
        registry.add("app.jwt.secret", () -> "dGVzdC1qd3Qtc2VjcmV0LWtleS0zMi1ieXRlcyEhISE=");
        registry.add("app.jwt.access-token-expiration", () -> "1800");
        registry.add("app.jwt.refresh-token-expiration", () -> "1209600");
        registry.add("app.toss.secret-key", () -> "test_sk_DUMMY");
    }

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
                .content("{\"verifiedToken\":\"" + vtoken + "\",\"name\":\"ConcUser\",\"password\":\"Test1234!\",\"gender\":\"MALE\"}")).andReturn();
        JsonNode data = objectMapper.readTree(sr.getResponse().getContentAsString()).get("data");
        String token = data.get("accessToken").asText();
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        String memberId = objectMapper.readTree(payload).get("sub").asText();
        return new String[]{token, memberId};
    }

    @Test
    @DisplayName("비관적 락: 잔여 1회 정기권에 2개 수업 동시 예약 → 1건만 성공, remaining=0")
    void pessimisticLock_concurrentDeduction() throws Exception {
        String[] auth = signup("01033330001");
        String token = auth[0]; String memberId = auth[1];

        // 강사
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"동시성강사\",\"phone\":\"010-0000-0001\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 수업 유형
        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"동시성유형\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 종류 + 발급 (잔여 1회)
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"동시성패스\",\"price\":100000,\"totalCount\":1,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":1,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // 수업 2개 (다른 시간대)
        LocalDate futureDate = LocalDate.now().plusDays(9);
        MvcResult cs1 = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"09:00\",\"endTime\":\"09:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId1 = objectMapper.readTree(cs1.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult cs2 = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"14:00\",\"endTime\":\"14:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId2 = objectMapper.readTree(cs2.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 동시 예약
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (Long cid : new Long[]{classId1, classId2}) {
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
        executor.awaitTermination(15, TimeUnit.SECONDS);

        // 1건 성공, 1건 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        // 정기권 잔여 0 확인
        MvcResult mship = mockMvc.perform(get("/api/members/me/memberships")
                .header("Authorization", "Bearer " + token)).andReturn();
        int remaining = objectMapper.readTree(mship.getResponse().getContentAsString())
                .get("data").get(0).get("remainingCount").asInt();
        assertThat(remaining).isEqualTo(0);
    }
}
