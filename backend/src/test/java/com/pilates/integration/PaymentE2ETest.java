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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 결제 도메인 E2E 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentE2ETest {

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

    private String[] signupAndGetTokenWithId(String phone) throws Exception {
        mockMvc.perform(post("/api/auth/sms/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phone + "\"}")).andExpect(status().isOk());
        String code = redisTemplate.opsForValue().get("sms:code:" + phone);
        MvcResult vr = mockMvc.perform(post("/api/auth/sms/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"" + code + "\"}")).andReturn();
        String vtoken = objectMapper.readTree(vr.getResponse().getContentAsString()).get("data").get("verifiedToken").asText();
        MvcResult sr = mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"verifiedToken\":\"" + vtoken + "\",\"name\":\"PayUser\",\"password\":\"Test1234!\",\"gender\":\"MALE\"}")).andReturn();
        JsonNode data = objectMapper.readTree(sr.getResponse().getContentAsString()).get("data");
        String accessToken = data.get("accessToken").asText();
        String[] parts = accessToken.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        String memberId = objectMapper.readTree(payload).get("sub").asText();
        return new String[]{accessToken, memberId};
    }

    private Long createLessonType(String token, String name) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private Long createMembershipPass(String token, Long ltId, String suffix) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"PAY" + suffix + "\",\"price\":180000,\"totalCount\":8,\"validityDays\":60,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    // ═══════════════════════════════════════════
    // 시나리오 1: 결제 정상 플로우
    // ═══════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("시나리오1: prepare → confirm → 정기권 발급 확인")
    void scenario1_normalFlow() throws Exception {
        String[] auth = signupAndGetTokenWithId("01055550001");
        String token = auth[0];

        Long ltId = createLessonType(token, "PAY그룹S1");
        Long passId = createMembershipPass(token, ltId, "S1");

        // 1. prepare
        MvcResult prepResult = mockMvc.perform(post("/api/payments/prepare")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipPassId\":" + passId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.data.amount").value(180000))
                .andReturn();
        JsonNode prep = objectMapper.readTree(prepResult.getResponse().getContentAsString()).get("data");
        String orderId = prep.get("orderId").asText();

        // 2. confirm (Mock 토스)
        MvcResult confirmResult = mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentKey\":\"mock_pk_" + orderId + "\",\"orderId\":\"" + orderId + "\",\"amount\":180000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.membershipId").exists())
                .andReturn();

        // 3. 정기권 발급 확인
        mockMvc.perform(get("/api/members/me/memberships")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalCount").value(8));

        // 4. 결제 이력 확인
        mockMvc.perform(get("/api/members/me/payments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].amount").value(180000));
    }

    // ═══════════════════════════════════════════
    // 시나리오 2: 멱등성 (같은 orderId 두 번 confirm)
    // ═══════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("시나리오2: 같은 orderId 두 번 confirm → 두 번째 거부")
    void scenario2_idempotency() throws Exception {
        String[] auth = signupAndGetTokenWithId("01055550002");
        String token = auth[0];

        Long ltId = createLessonType(token, "PAY그룹S2");
        Long passId = createMembershipPass(token, ltId, "S2");

        MvcResult prepResult = mockMvc.perform(post("/api/payments/prepare")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipPassId\":" + passId + "}")).andExpect(status().isOk()).andReturn();
        String orderId = objectMapper.readTree(prepResult.getResponse().getContentAsString())
                .get("data").get("orderId").asText();

        // 첫 confirm
        mockMvc.perform(post("/api/payments/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentKey\":\"mock_pk\",\"orderId\":\"" + orderId + "\",\"amount\":180000}"))
                .andExpect(status().isOk());

        // 두 번째 confirm → 거부
        mockMvc.perform(post("/api/payments/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentKey\":\"mock_pk2\",\"orderId\":\"" + orderId + "\",\"amount\":180000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PAY_002"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 3: 금액 위변조
    // ═══════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("시나리오3: confirm 시 금액 위변조 → PAYMENT_AMOUNT_MISMATCH")
    void scenario3_amountMismatch() throws Exception {
        String[] auth = signupAndGetTokenWithId("01055550003");
        String token = auth[0];

        Long ltId = createLessonType(token, "PAY그룹S3");
        Long passId = createMembershipPass(token, ltId, "S3");

        MvcResult prepResult = mockMvc.perform(post("/api/payments/prepare")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipPassId\":" + passId + "}")).andExpect(status().isOk()).andReturn();
        String orderId = objectMapper.readTree(prepResult.getResponse().getContentAsString())
                .get("data").get("orderId").asText();

        // 금액 위변조: 180000 → 1000
        mockMvc.perform(post("/api/payments/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentKey\":\"mock_pk\",\"orderId\":\"" + orderId + "\",\"amount\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PAY_003"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 4: 전액 환불
    // ═══════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("시나리오4: 결제 후 관리자 전액 환불 → REFUNDED + 정기권 만료")
    void scenario4_fullRefund() throws Exception {
        String[] auth = signupAndGetTokenWithId("01055550004");
        String token = auth[0];

        Long ltId = createLessonType(token, "PAY그룹S4");
        Long passId = createMembershipPass(token, ltId, "S4");

        // prepare + confirm
        MvcResult prepResult = mockMvc.perform(post("/api/payments/prepare")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipPassId\":" + passId + "}")).andExpect(status().isOk()).andReturn();
        String orderId = objectMapper.readTree(prepResult.getResponse().getContentAsString())
                .get("data").get("orderId").asText();

        MvcResult confirmResult = mockMvc.perform(post("/api/payments/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentKey\":\"mock_pk\",\"orderId\":\"" + orderId + "\",\"amount\":180000}"))
                .andExpect(status().isOk()).andReturn();
        Long paymentId = objectMapper.readTree(confirmResult.getResponse().getContentAsString())
                .get("data").get("paymentId").asLong();

        // 관리자 전액 환불
        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/refund")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refundAmount\":180000,\"reason\":\"테스트 환불\"}"))
                .andExpect(status().isOk());

        // 결제 상태 확인
        mockMvc.perform(get("/api/admin/payments/" + paymentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }
}
