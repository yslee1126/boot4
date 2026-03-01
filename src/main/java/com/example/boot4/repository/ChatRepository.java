package com.example.boot4.repository;

import com.example.boot4.domain.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByRoomIdOrderByCreatedAtAsc(Long roomId);
}
