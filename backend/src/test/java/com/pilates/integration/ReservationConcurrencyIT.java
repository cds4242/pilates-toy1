package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.integration.support.AuthTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예약 동시성 통합 테스트 (Docker MySQL + Redis 필요).
 * local 프로파일 사용 — Docker Compose의 pilates-mysql + pilates-redis 컨테이너 필요.
 * CI: CONCURRENCY_TEST_ENABLED=true 환경변수로 활성화.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/pilates_concurrency_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&createDatabaseIfNotExist=true",
        "spring.datasource.username=pilates",
        "spring.datasource.password=pilates1234",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.clean-disabled=false",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "app.encryption.key=dGVzdC1rZXktMzItYnl0ZXMtbG9uZy1leGFtcGxlISE=",
        "app.encryption.key-version=v1",
        "app.jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWtleS0zMi1ieXRlcyEhISE=",
        "app.jwt.access-token-expiration=1800",
        "app.jwt.refresh-token-expiration=1209600",
        "app.toss.secret-key=test_sk_DUMMY",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.logging.masking.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationConcurrencyIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRepository adminRepository;
    @Autowired private InstructorRepository instructorRepository;

    private AuthTestHelper authHelper;

    @BeforeEach
    void setUp() {
        authHelper = new AuthTestHelper(mockMvc, objectMapper, redisTemplate,
                passwordEncoder, adminRepository, instructorRepository);
        authHelper.clearRedis();
    }

    @Test
    @DisplayName("비관적 락: 잔여 1회 정기권에 2개 수업 동시 예약 → 1건만 성공, remaining=0")
    void pessimisticLock_concurrentDeduction() throws Exception {
        // --- 관리자 토큰 발급 ---
        String adminToken = authHelper.loginAsAdmin();

        // --- 회원 토큰 발급 ---
        String phone = "010" + String.valueOf(System.currentTimeMillis()).substring(5);
        String[] memberAuth = authHelper.loginAsMember(phone);
        String memberToken = memberAuth[0];
        String memberId = memberAuth[1];

        // --- 관리자 API: 강사 생성 ---
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"동시성강사" + System.currentTimeMillis() + "\",\"phone\":\"010-0000-0001\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        // --- 관리자 API: 수업 유형 생성 ---
        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"동시성유형" + System.currentTimeMillis() + "\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        // --- 관리자 API: 수강권 템플릿 생성 ---
        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"동시성패스" + System.currentTimeMillis() + "\",\"price\":100000,\"totalCount\":1,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        // --- 관리자 API: 회원 수강권 부여 ---
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":1,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // --- 관리자 API: 수업 일정 2건 생성 ---
        LocalDate futureDate = LocalDate.now().plusDays(9);
        MvcResult cs1 = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"09:00\",\"endTime\":\"09:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId1 = objectMapper.readTree(cs1.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult cs2 = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"14:00\",\"endTime\":\"14:50\",\"maxCapacity\":8}")).andExpect(status().isOk()).andReturn();
        Long classId2 = objectMapper.readTree(cs2.getResponse().getContentAsString()).get("data").get("id").asLong();

        // --- 동시성 테스트: 회원 토큰으로 예약 ---
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
                            .header("Authorization", "Bearer " + memberToken)
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

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        MvcResult mship = mockMvc.perform(get("/api/members/me/memberships")
                .header("Authorization", "Bearer " + memberToken)).andReturn();
        int remaining = objectMapper.readTree(mship.getResponse().getContentAsString())
                .get("data").get(0).get("remainingCount").asInt();
        assertThat(remaining).isEqualTo(0);
    }
}
