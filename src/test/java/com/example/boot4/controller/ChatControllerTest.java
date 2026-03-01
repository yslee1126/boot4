package com.example.boot4.controller;

import com.example.boot4.domain.Chat;
import com.example.boot4.domain.Room;
import com.example.boot4.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatControllerTest {

    private MockMvc mockMvc;
    private ChatService chatService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        ChatController chatController = new ChatController(chatService);
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
        objectMapper = new ObjectMapper();
    }

    private Room makeRoom(Long id) {
        try {
            Room room = new Room("테스트방");
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

    private Chat makeChat(Long id, Room room, Chat.Role role, String message) {
        try {
            Chat chat = new Chat(room, role, message);
            var idField = Chat.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(chat, id);
            var createdAtField = Chat.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(chat, LocalDateTime.of(2026, 3, 1, 10, 1));
            return chat;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("POST /api/rooms/{roomId}/chats - 대화 저장")
    void addChat() throws Exception {
        Room room = makeRoom(1L);
        Chat chat = makeChat(10L, room, Chat.Role.USER, "안녕하세요!");

        given(chatService.addChat(eq(1L), eq(Chat.Role.USER), any())).willReturn(chat);

        mockMvc.perform(post("/api/rooms/1/chats")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                        new ChatController.ChatRequest(Chat.Role.USER, "안녕하세요!"))))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.roomId").value(1L))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.message").value("안녕하세요!"));
    }

    @Test
    @DisplayName("GET /api/rooms/{roomId}/chats - 대화 목록 조회")
    void getChats() throws Exception {
        Room room = makeRoom(1L);
        List<Chat> chats = List.of(
                makeChat(10L, room, Chat.Role.USER, "안녕하세요!"),
                makeChat(11L, room, Chat.Role.ASSISTANT, "무엇을 도와드릴까요?"));

        given(chatService.getChats(1L)).willReturn(chats);

        mockMvc.perform(get("/api/rooms/1/chats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$[1].message").value("무엇을 도와드릴까요?"));
    }

    @Test
    @DisplayName("GET /api/rooms/{roomId}/chats/{chatId} - 대화 단건 조회")
    void getChat() throws Exception {
        Room room = makeRoom(1L);
        Chat chat = makeChat(10L, room, Chat.Role.ASSISTANT, "안녕하세요!");

        given(chatService.getChat(10L)).willReturn(chat);

        mockMvc.perform(get("/api/rooms/1/chats/10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.role").value("ASSISTANT"));
    }
}
