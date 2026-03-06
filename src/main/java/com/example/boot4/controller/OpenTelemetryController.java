package com.example.boot4.controller;

import com.example.boot4.service.SubscriptionService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/otel")
@RequiredArgsConstructor
public class OpenTelemetryController {

    private final SubscriptionService subscriptionService;

    /**
     * OpenTelemetry 테스트 엔드포인트
     *
     * Trace 구조:
     * - http get /api/otel/test (Spring MVC 자동)
     *   ├─ otel.test.request (@Observed)
     *   └─ subscription.getUserSubscriptions (Service @Observed)
     */
    @Observed(name = "otel.test.request")
    @GetMapping("/test")
    public Map<String, Object> test(@RequestParam(defaultValue = "1") Long userId) {
        log.info("Processing OTEL test request for user: {}", userId);

        var subscriptions = subscriptionService.getUserSubscriptions(userId);

        return Map.of(
            "userId", userId,
            "subscriptionCount", subscriptions.size(),
            "status", "success"
        );
    }

    /**
     * 에러 테스트 엔드포인트
     * Exception이 발생해도 trace에 기록됨
     */
    @Observed(name = "otel.test.error")
    @GetMapping("/error")
    public Map<String, String> error() {
        log.error("Simulating an error for tracing test");
        throw new RuntimeException("Test exception for OpenTelemetry tracing");
    }
}
