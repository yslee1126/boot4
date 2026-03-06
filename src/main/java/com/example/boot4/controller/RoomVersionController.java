package com.example.boot4.controller;

import com.example.boot4.filter.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API Versioning 네이티브 지원 실험용 컨트롤러
 *
 * 동일한 경로 /api/rooms/info 에 대해 버전별로 다른 응답을 반환한다.
 * 버전은 X-API-Version 헤더로 구분된다. (ApiVersioningConfig 참고)
 *
 * - X-API-Version: 1.0 → infoV1() 호출
 * - X-API-Version: 2.0 → infoV2() 호출
 * - 헤더 없음 → defaultVersion(1.0) → infoV1() 호출
 *
 * [Java 25 Scoped Values 실험]
 * RequestIdFilter에서 바인딩한 requestId를 ThreadLocal/MDC 없이 ScopedValue로 읽는다.
 * REQUEST_ID.isBound() 로 필터 범위 외부 호출 여부도 안전하게 확인 가능.
 *
 * OpenTelemetry와 완전히 분리된 순수 ScopedValue 기능 테스트용
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomVersionController {

    private static final Logger log = LoggerFactory.getLogger(RoomVersionController.class);

    /**
     * v1: 기본 응답 (version, note, requestId 필드)
     * curl -v "http://localhost:8080/api/rooms/info" -H "X-API-Version: 1.0"
     */
    @GetMapping(path = "/info", version = "1.0")
    public Map<String, String> infoV1() {
        String requestId = resolveRequestId();
        log.info("[RoomVersionController] infoV1() 호출 | requestId={} | thread={}",
                requestId, Thread.currentThread());

        Map<String, String> result = new LinkedHashMap<>();
        result.put("version", "1.0");
        result.put("note", "기본 응답 (v1)");
        result.put("requestId", requestId);
        return result;
    }

    /**
     * v2: 확장 응답 (extra 필드 추가)
     * 예) 신규 클라이언트용 필드가 추가된 버전
     * curl -v "http://localhost:8080/api/rooms/info" -H "X-API-Version: 2.0"
     */
    @GetMapping(path = "/info", version = "2.0")
    public Map<String, String> infoV2() {
        String requestId = resolveRequestId();
        log.info("[RoomVersionController] infoV2() 호출 | requestId={} | thread={}",
                requestId, Thread.currentThread());

        Map<String, String> result = new LinkedHashMap<>();
        result.put("version", "2.0");
        result.put("note", "확장 응답 (v2)");
        result.put("extra", "신규 필드 - v2에서 추가됨");
        result.put("requestId", requestId);
        return result;
    }

    /**
     * ScopedValue에서 requestId를 읽는 헬퍼.
     * 필터 범위 밖(예: 단위 테스트)에서 호출 시 "N/A"를 반환하여 NPE 방지.
     */
    private String resolveRequestId() {
        return RequestContext.REQUEST_ID.isBound()
                ? RequestContext.REQUEST_ID.get()
                : "N/A (ScopedValue unbound)";
    }
}
