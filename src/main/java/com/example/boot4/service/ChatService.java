package com.example.boot4.service;

import com.example.boot4.domain.Chat;
import com.example.boot4.domain.Room;
import com.example.boot4.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final RoomService roomService;

    @Transactional
    public Chat addChat(Long roomId, Chat.Role role, String message) {
        Room room = roomService.getRoom(roomId);
        return chatRepository.save(new Chat(room, role, message));
    }

    public List<Chat> getChats(Long roomId) {
        // room 존재 여부 확인
        roomService.getRoom(roomId);
        return chatRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }

    public Chat getChat(Long chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));
    }
}
