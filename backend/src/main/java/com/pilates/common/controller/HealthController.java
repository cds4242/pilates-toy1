package com.pilates.common.controller;

import com.pilates.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 애플리케이션 헬스체크 API.
 * 로드밸런서, 모니터링 도구에서 서버 상태 확인용.
 */
@Tag(name = "Health", description = "서버 상태 확인")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${app.version:unknown}")
    private String version;

    @Operation(summary = "헬스체크", description = "서버 상태, 버전, 타임스탬프를 반환한다.")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "version", version,
                "timestamp", LocalDateTime.now()
        ));
    }
}
