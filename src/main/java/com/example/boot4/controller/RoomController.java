package com.example.boot4.controller;

import com.example.boot4.domain.Room;
import com.example.boot4.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    public record RoomRequest(String title) {
    }

    public record RoomResponse(Long id, String title, String createdAt) {
        public static RoomResponse from(Room room) {
            return new RoomResponse(
                    room.getId(),
                    room.getTitle(),
                    room.getCreatedAt().toString());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@RequestBody RoomRequest request) {
        return RoomResponse.from(roomService.createRoom(request.title()));
    }

    @GetMapping
    public List<RoomResponse> getAllRooms() {
        return roomService.getAllRooms().stream()
                .map(RoomResponse::from)
                .toList();
    }

    @GetMapping("/{roomId}")
    public RoomResponse getRoom(@PathVariable Long roomId) {
        return RoomResponse.from(roomService.getRoom(roomId));
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable Long roomId) {
        roomService.deleteRoom(roomId);
    }
}
