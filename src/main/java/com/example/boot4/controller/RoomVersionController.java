package com.example.boot4.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomVersionController {

    /**
     * v1: 기본 응답 (version, note 필드)
     */
    @GetMapping(path = "/info", version = "1.0")
    public Map<String, String> infoV1() {
        return Map.of(
                "version", "1.0",
                "note", "기본 응답 (v1)");
    }

    /**
     * v2: 확장 응답 (extra 필드 추가)
     * 예) 신규 클라이언트용 필드가 추가된 버전
     */
    @GetMapping(path = "/info", version = "2.0")
    public Map<String, String> infoV2() {
        return Map.of(
                "version", "2.0",
                "note", "확장 응답 (v2)",
                "extra", "신규 필드 - v2에서 추가됨");
    }
}
