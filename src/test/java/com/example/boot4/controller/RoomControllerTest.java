package com.example.boot4.controller;

import com.example.boot4.domain.Room;
import com.example.boot4.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoomControllerTest {

    private MockMvc mockMvc;
    private RoomService roomService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        roomService = mock(RoomService.class);
        RoomController roomController = new RoomController(roomService);
        mockMvc = MockMvcBuilders.standaloneSetup(roomController).build();
        objectMapper = new ObjectMapper();
    }

    private Room makeRoom(Long id, String title) {
        try {
            Room room = new Room(title);
            var idField = Room.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(room, id);
            var createdAtField = Room.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(room, LocalDateTime.of(2026, 3, 1, 10, 0));
            return room;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("POST /api/rooms - 채팅방 생성")
    void createRoom() throws Exception {
        Room room = makeRoom(1L, "테스트 채팅방");
        given(roomService.createRoom(anyString())).willReturn(room);

        mockMvc.perform(post("/api/rooms")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                        new RoomController.RoomRequest("테스트 채팅방"))))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("테스트 채팅방"));
    }

    @Test
    @DisplayName("GET /api/rooms - 전체 채팅방 조회")
    void getAllRooms() throws Exception {
        given(roomService.getAllRooms()).willReturn(List.of(
                makeRoom(1L, "방1"),
                makeRoom(2L, "방2")));

        mockMvc.perform(get("/api/rooms"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("방1"))
                .andExpect(jsonPath("$[1].title").value("방2"));
    }

    @Test
    @DisplayName("GET /api/rooms/{roomId} - 채팅방 단건 조회")
    void getRoom() throws Exception {
        given(roomService.getRoom(1L)).willReturn(makeRoom(1L, "방1"));

        mockMvc.perform(get("/api/rooms/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("방1"));
    }

    @Test
    @DisplayName("DELETE /api/rooms/{roomId} - 채팅방 삭제")
    void deleteRoom() throws Exception {
        willDoNothing().given(roomService).deleteRoom(1L);

        mockMvc.perform(delete("/api/rooms/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}
