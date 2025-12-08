package com.service.sys_srv.chat.entity;


import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "chat_participant")
public class ChatParticipant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;


  @Column(name = "room_id")
  private Long roomId;


  @Column(name = "user_id")
  private String userId;


  private String role; // PATIENT | DOCTOR


  @Column(name = "joined_at")
  private LocalDateTime joinedAt = LocalDateTime.now();

}