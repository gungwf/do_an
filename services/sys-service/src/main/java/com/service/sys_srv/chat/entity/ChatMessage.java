package com.service.sys_srv.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "chat_message")
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;


  @Column(name = "room_id")
  private Long roomId;


  @Column(name = "sender_id")
  private String senderId;


  @Column(columnDefinition = "TEXT")
  private String content;


  @Column(name = "message_type")
  private String messageType = "TEXT"; // TEXT, FILE, SYSTEM


  @Column(name = "metadata", columnDefinition = "TEXT")
  private String metadata; // JSON string if needed


  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "delivered")
  private boolean delivered = false;

  @Column(name = "read")
  private boolean read = false;

  public Boolean getDelivered() {
    return delivered;
  }

  public Boolean getRead() {
    return read;
  }
}