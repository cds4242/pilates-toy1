package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 회원 도메인 E2E 통합 테스트.
 * H2 + Redis (Docker) 환경에서 실행.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;

    @BeforeEach
    void clearRedis() {
        // 테스트 간 SMS Rate Limit 키 정리
        Set<String> smsKeys = redisTemplate.keys("sms:*");
        if (smsKeys != null && !smsKeys.isEmpty()) {
            redisTemplate.delete(smsKeys);
        }
        Set<String> authKeys = redisTemplate.keys("auth:*");
        if (authKeys != null && !authKeys.isEmpty()) {
            redisTemplate.delete(authKeys);
        }
    }

    private String smsAndVerify(String phone) throws Exception {
        // SMS 발송
        MvcResult smsResult = mockMvc.perform(post("/api/auth/sms/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\"}"))
                .andReturn();
        assertThat(smsResult.getResponse().getStatus())
                .as("SMS 발송 응답 (phone=%s): %s", phone, smsResult.getResponse().getContentAsString())
                .isEqualTo(200);

        // Redis에서 인증번호 직접 추출
        String code = redisTemplate.opsForValue().get("sms:code:" + phone);
        assertThat(code).isNotNull().hasSize(6);

        // 인증 검증
        MvcResult verifyResult = mockMvc.perform(post("/api/auth/sms/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verifiedToken").exists())
                .andReturn();

        JsonNode node = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        return node.get("data").get("verifiedToken").asText();
    }

    // ═══════════════════════════════════════════
    // 시나리오 1: 회원 풀 라이프사이클
    // ═══════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("시나리오1: SMS인증→가입→조회→사진업로드→수정→로그인")
    void scenario1_fullLifecycle() throws Exception {
        String phone = "01099990001";

        // 1. SMS 인증
        String verifiedToken = smsAndVerify(phone);

        // 2. 회원가입
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedToken\":\"" + verifiedToken + "\",\"name\":\"TestUser1\",\"password\":\"Test1234!\",\"gender\":\"FEMALE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicId").exists())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        JsonNode signup = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String accessToken = signup.get("data").get("accessToken").asText();

        // 3. 내 정보 조회
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("TestUser1"))
                .andExpect(jsonPath("$.data.phoneNumber").value(phone))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 4. 프로필 사진 업로드
        byte[] pngBytes = new ClassPathResource("fixtures/test-profile.png").getInputStream().readAllBytes();
        MockMultipartFile imageFile = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

        mockMvc.perform(multipart("/api/members/me/profile-image")
                        .file(imageFile)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl").exists());

        // 5. 조회 시 profileImageUrl 존재
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").isNotEmpty());

        // 6. 정보 수정
        mockMvc.perform(patch("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ModifiedUser\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("ModifiedUser"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 2: 비밀번호 재설정
    // ═══════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("시나리오2: 비밀번호 재설정 → 새 비밀번호 로그인 → 토큰 재사용 차단")
    void scenario2_passwordReset() throws Exception {
        String phone = "01099990002";

        // 사전: 회원가입
        String vtoken1 = smsAndVerify(phone);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedToken\":\"" + vtoken1 + "\",\"name\":\"ResetUser\",\"password\":\"Old1234!\",\"gender\":\"MALE\"}"))
                .andExpect(status().isOk());

        // 1. SMS 인증 (비번 재설정용)
        // Rate limit 우회: 기존 SMS 관련 키 삭제
        redisTemplate.delete("sms:rate:" + phone);
        redisTemplate.delete("sms:daily:" + phone);
        String vtoken2 = smsAndVerify(phone);

        // 2. 비밀번호 재설정
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedToken\":\"" + vtoken2 + "\",\"newPassword\":\"New1234!\"}"))
                .andExpect(status().isOk());

        // 3. 새 비밀번호로 로그인
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"password\":\"New1234!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());

        // 4. 이전 비밀번호로 로그인 실패
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"password\":\"Old1234!\"}"))
                .andExpect(status().isUnauthorized());

        // 5. 같은 verifiedToken 재사용 차단
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedToken\":\"" + vtoken2 + "\",\"newPassword\":\"Another1!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SMS_005"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 3: 탈퇴 + 재가입
    // ═══════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("시나리오3: 탈퇴 → 같은 토큰 사용 불가 → 같은 번호 재가입")
    void scenario3_withdrawAndReregister() throws Exception {
        String phone = "01099990003";

        // 사전: 가입 + 로그인
        String vtoken = smsAndVerify(phone);
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedToken\":\"" + vtoken + "\",\"name\":\"WithdrawUser\",\"password\":\"Test1234!\",\"gender\":\"FEMALE\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(signupResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // 1. 탈퇴
        mockMvc.perform(delete("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("reason", "E2E test withdrawal"))
                .andExpect(status().isOk());

        // 2. 같은 비밀번호로 로그인 시도 → 실패
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"password\":\"Test1234!\"}"))
                .andExpect(status().isUnauthorized());

        // 3. 같은 번호로 재가입
        redisTemplate.delete("sms:rate:" + phone);
        redisTemplate.delete("sms:daily:" + phone);
        String vtoken2 = smsAndVerify(phone);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedToken\":\"" + vtoken2 + "\",\"name\":\"ReregisteredUser\",\"password\":\"Test1234!\",\"gender\":\"FEMALE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicId").exists());
    }

    // ═══════════════════════════════════════════
    // 시나리오 4: Refresh Rotation 보안
    // ═══════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("시나리오4: Refresh Rotation → 이전 토큰 재사용 시 차단")
    void scenario4_refreshRotation() throws Exception {
        String phone = "01099990004";

        // 사전: 가입
        String vtoken = smsAndVerify(phone);
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verifiedToken\":\"" + vtoken + "\",\"name\":\"RotationUser\",\"password\":\"Test1234!\",\"gender\":\"MALE\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode signup = objectMapper.readTree(signupResult.getResponse().getContentAsString()).get("data");
        String refreshToken1 = signup.get("refreshToken").asText();

        // 1. Refresh 토큰으로 갱신
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken1 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        JsonNode refresh = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("data");
        String accessToken2 = refresh.get("accessToken").asText();
        String refreshToken2 = refresh.get("refreshToken").asText();

        // 2. 새 Access 토큰은 유효
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken2))
                .andExpect(status().isOk());

        // 3. 이전 Refresh 토큰 재사용 → 차단 + 모든 세션 무효화 (보안)
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken1 + "\"}"))
                .andExpect(status().isUnauthorized());

        // 4. 재사용 감지 후 새 Refresh 토큰도 무효화됨 (의도된 보안 동작)
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken2 + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
