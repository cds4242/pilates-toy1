package com.pilates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 애플리케이션 컨텍스트 로딩 테스트.
 * H2 인메모리 DB로 테스트하여 Docker 의존 없이 실행 가능.
 */
@SpringBootTest
@ActiveProfiles("test")
class PilatesApplicationTests {

    @Test
    @DisplayName("Spring 컨텍스트가 정상적으로 로딩된다")
    void contextLoads() {
    }
}
