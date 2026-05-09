package com.pilates.integration.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilates.domain.admin.entity.Admin;
import com.pilates.domain.admin.entity.AdminRole;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.instructor.entity.Instructor;
import com.pilates.domain.instructor.repository.InstructorRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E 테스트용 인증 헬퍼.
 * 역할별 JWT 토큰 발급을 간편하게 지원한다.
 */
public class AuthTestHelper {

    private static final String DEFAULT_PASSWORD = "Test1234!";
    private static final java.util.concurrent.atomic.AtomicLong adminCounter = new java.util.concurrent.atomic.AtomicLong(System.nanoTime());

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;
    private final InstructorRepository instructorRepository;

    public AuthTestHelper(MockMvc mockMvc, ObjectMapper objectMapper,
                          StringRedisTemplate redisTemplate, PasswordEncoder passwordEncoder,
                          AdminRepository adminRepository, InstructorRepository instructorRepository) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.adminRepository = adminRepository;
        this.instructorRepository = instructorRepository;
    }

    /**
     * 회원 가입 + 로그인 → [accessToken, memberId]
     */
    public String[] loginAsMember(String phone) throws Exception {
        mockMvc.perform(post("/api/auth/sms/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phone + "\"}")).andExpect(status().isOk());
        String code = redisTemplate.opsForValue().get("sms:code:" + phone);
        MvcResult vr = mockMvc.perform(post("/api/auth/sms/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"" + code + "\"}")).andReturn();
        String vtoken = objectMapper.readTree(vr.getResponse().getContentAsString()).get("data").get("verifiedToken").asText();
        MvcResult sr = mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"verifiedToken\":\"" + vtoken + "\",\"name\":\"TestUser\",\"password\":\"" + DEFAULT_PASSWORD + "\",\"gender\":\"MALE\"}")).andReturn();
        JsonNode data = objectMapper.readTree(sr.getResponse().getContentAsString()).get("data");
        String token = data.get("accessToken").asText();
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        String memberId = objectMapper.readTree(payload).get("sub").asText();
        return new String[]{token, memberId};
    }

    /**
     * ADMIN 역할 admin 계정 생성 + 로그인 → accessToken
     */
    public String loginAsAdmin() throws Exception {
        return loginAsAdminWithRole(AdminRole.ADMIN, null);
    }

    /**
     * SUPER_ADMIN 역할 admin 계정 생성 + 로그인 → accessToken
     */
    public String loginAsSuperAdmin() throws Exception {
        return loginAsAdminWithRole(AdminRole.SUPER_ADMIN, null);
    }

    /**
     * INSTRUCTOR 역할 admin 계정 생성 + 로그인 → accessToken.
     * instructorId로 instructors 테이블과 연결.
     */
    public String loginAsInstructor(Long instructorId) throws Exception {
        return loginAsAdminWithRole(AdminRole.INSTRUCTOR, instructorId);
    }

    private String loginAsAdminWithRole(AdminRole role, Long instructorId) throws Exception {
        long seq = adminCounter.incrementAndGet();
        String loginId = "test_" + role.name().toLowerCase() + "_" + seq;

        Instructor instructor = null;
        if (instructorId != null) {
            instructor = instructorRepository.findById(instructorId).orElseThrow();
        }

        Admin admin = Admin.builder()
                .loginId(loginId)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .name("Test" + role.name() + seq)
                .role(role)
                .instructor(instructor)
                .active(true)
                .build();
        adminRepository.save(admin);

        MvcResult lr = mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + DEFAULT_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(lr.getResponse().getContentAsString()).get("data").get("accessToken").asText();
    }

    /** Redis 클리어 (각 테스트 @BeforeEach용) */
    public void clearRedis() {
        Set<String> keys = redisTemplate.keys("sms:*");
        if (keys != null) redisTemplate.delete(keys);
        Set<String> authKeys = redisTemplate.keys("auth:*");
        if (authKeys != null) redisTemplate.delete(authKeys);
    }
}
