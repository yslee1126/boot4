package com.example.boot4.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenTelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testOtelEndpoint() throws Exception {
        mockMvc.perform(get("/api/otel/test")
                        .param("userId", "1"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.subscriptionCount").exists());
    }

    @Test
    void testErrorEndpoint() throws Exception {
        // RuntimeException이 발생하면 MockMvc가 ServletException으로 감싸서 던짐
        // 에러가 trace에 기록되는지 확인하는 것이 목적이므로 예외 발생 자체를 검증
        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> mockMvc.perform(get("/api/otel/error"))
        );
    }
}
