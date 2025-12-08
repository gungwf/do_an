package com.service.sys_srv.chat.repository;

import com.service.sys_srv.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
  List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId, Pageable pageable);
}