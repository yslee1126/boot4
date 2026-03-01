package com.example.boot4.controller;

import com.example.boot4.domain.Chat;
import com.example.boot4.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    public record ChatRequest(Chat.Role role, String message) {
    }

    public record ChatResponse(Long id, Long roomId, String role, String message, String createdAt) {
        public static ChatResponse from(Chat chat) {
            return new ChatResponse(
                    chat.getId(),
                    chat.getRoom().getId(),
                    chat.getRole().name(),
                    chat.getMessage(),
                    chat.getCreatedAt().toString());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatResponse addChat(
            @PathVariable Long roomId,
            @RequestBody ChatRequest request) {
        return ChatResponse.from(chatService.addChat(roomId, request.role(), request.message()));
    }

    @GetMapping
    public List<ChatResponse> getChats(@PathVariable Long roomId) {
        return chatService.getChats(roomId).stream()
                .map(ChatResponse::from)
                .toList();
    }

    @GetMapping("/{chatId}")
    public ChatResponse getChat(
            @PathVariable Long roomId,
            @PathVariable Long chatId) {
        return ChatResponse.from(chatService.getChat(chatId));
    }
}
