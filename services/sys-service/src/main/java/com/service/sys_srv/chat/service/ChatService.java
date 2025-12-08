package com.service.sys_srv.chat.service;

import com.service.sys_srv.chat.dto.ChatMessageDto;
import java.util.List;

public interface ChatService {
  ChatMessageDto saveMessage(ChatMessageDto dto);
  List<ChatMessageDto> getRecentMessages(Long roomId, int limit);

  ChatMessageDto markDelivered(Long messageId, String deliveredByUserId);
  ChatMessageDto markRead(Long messageId, String readByUserId);
}