package com.example.boot4.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Framework 7 (Boot 4) 네이티브 API Versioning 통합 테스트
 *
 * Boot 4 에서 @AutoConfigureMockMvc 패키지 변경:
 * 이전:
 * org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
 * 이후: org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
 * (spring-boot-starter-webmvc-test 의존성 필요)
 *
 * @SpringBootTest 로 전체 컨텍스트를 로드하여 ApiVersioningConfig.configureApiVersioning()
 *                 이
 *                 실제 MVC 설정에 반영된 상태로 테스트한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiVersioningTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("X-API-Version: 1.0 헤더 → v1 응답 반환 (extra 필드 없음)")
    void requestWithV1Header_returnsV1Response() throws Exception {
        mockMvc.perform(get("/api/rooms/info")
                .header("X-API-Version", "1.0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.note").value("기본 응답 (v1)"))
                .andExpect(jsonPath("$.extra").doesNotExist());
    }

    @Test
    @DisplayName("X-API-Version: 2.0 헤더 → v2 응답 반환 (extra 필드 포함)")
    void requestWithV2Header_returnsV2Response() throws Exception {
        mockMvc.perform(get("/api/rooms/info")
                .header("X-API-Version", "2.0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.extra").value("신규 필드 - v2에서 추가됨"));
    }

    @Test
    @DisplayName("버전 헤더 없이 요청 → defaultVersion(1.0) 으로 v1 응답 반환")
    void requestWithoutVersionHeader_fallsBackToDefaultV1() throws Exception {
        mockMvc.perform(get("/api/rooms/info"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0"));
    }

    @Test
    @DisplayName("존재하지 않는 버전(9.9) 요청 → 400 Bad Request (InvalidApiVersionException)")
    void requestWithUnsupportedVersion_returns400() throws Exception {
        // Spring Framework 7 실제 동작:
        // "9.9" → SemanticApiVersionParser 가 "9.9.0" 으로 normalize 후
        // 등록된 핸들러(1.0, 2.0) 와 매칭되지 않아 InvalidApiVersionException(400) 발생
        mockMvc.perform(get("/api/rooms/info")
                .header("X-API-Version", "9.9"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
