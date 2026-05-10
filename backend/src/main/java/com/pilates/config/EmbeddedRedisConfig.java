package com.pilates.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

/**
 * 포트폴리오 프로파일 전용 임베디드 Redis.
 * JAR 단독 실행 시 외부 Redis 없이도 SMS 인증 / Refresh Token / 웹훅 멱등성이 동작하게 한다.
 */
@Slf4j
@Configuration
@Profile("portfolio")
public class EmbeddedRedisConfig {

    @Value("${spring.data.redis.port:16379}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void start() {
        // 포트가 이미 사용 중이면 재시작/중복 기동 시 죽지 않도록 그대로 진행한다.
        try {
            redisServer = RedisServer.newRedisServer()
                    .port(redisPort)
                    .setting("maxmemory 64M")
                    .build();
            redisServer.start();
            log.info("[Portfolio] 임베디드 Redis 기동 완료 (port={})", redisPort);
        } catch (Exception e) {
            log.warn("[Portfolio] 임베디드 Redis 기동 실패 — 외부 Redis가 이미 떠있다면 그것을 사용한다. 사유: {}",
                    e.getMessage());
            redisServer = null;
        }
    }

    @PreDestroy
    public void stop() {
        if (redisServer == null) return;
        try {
            redisServer.stop();
            log.info("[Portfolio] 임베디드 Redis 종료");
        } catch (Exception e) {
            log.warn("[Portfolio] 임베디드 Redis 종료 실패: {}", e.getMessage());
        }
    }
}
