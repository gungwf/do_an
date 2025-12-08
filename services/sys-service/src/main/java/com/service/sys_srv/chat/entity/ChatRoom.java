package com.service.sys_srv.chat.entity;


import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "chat_room")
public class ChatRoom {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;


  @Column(nullable = false)
  private String type; // ONE_TO_ONE, GROUP


  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  // Transient field for participants (not stored in DB, loaded manually)
  @Transient
  private List<ChatParticipant> participants;

}