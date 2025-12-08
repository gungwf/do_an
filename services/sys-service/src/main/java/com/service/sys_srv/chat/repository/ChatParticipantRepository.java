package com.service.sys_srv.chat.repository;

import com.service.sys_srv.chat.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
  List<ChatParticipant> findByRoomId(Long roomId);
  List<ChatParticipant> findByUserId(String userId);

  // IMPORTANT: kiểu userId là String nếu bạn dùng UUID/text user id
  boolean existsByRoomIdAndUserId(Long roomId, String userId);
}