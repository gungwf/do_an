package com.service.sys_srv.chat.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatMessageDto {
  private Long id;
  private Long roomId;
  private String senderId;
  private String senderName; // Name of the sender (loaded from User)
  private String content;
  private String messageType;
  private String metadata;
  private LocalDateTime createdAt;
  private boolean delivered;
  private boolean read;
}