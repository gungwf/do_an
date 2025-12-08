package com.service.sys_srv.chat.service;

import com.service.sys_srv.chat.entity.ChatParticipant;
import com.service.sys_srv.chat.entity.ChatRoom;
import com.service.sys_srv.chat.repository.ChatParticipantRepository;
import com.service.sys_srv.chat.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatRoomService {

  private final ChatRoomRepository roomRepository;
  private final ChatParticipantRepository participantRepository;
  private  final PresenceService presenceService ;

  public ChatRoomService(ChatRoomRepository roomRepository, ChatParticipantRepository participantRepository,
      PresenceService presenceService) {
    this.roomRepository = roomRepository;
    this.participantRepository = participantRepository;
    this.presenceService = presenceService;
  }

  @Transactional
  public ChatRoom createOneToOne(String userA, String userB) {
    ChatRoom room = new ChatRoom();
    room.setType("ONE_TO_ONE");
    room = roomRepository.save(room);

    ChatParticipant p1 = new ChatParticipant();
    p1.setRoomId(room.getId());
    p1.setUserId(userA);
    p1.setRole("PATIENT");
    participantRepository.save(p1);

    ChatParticipant p2 = new ChatParticipant();
    p2.setRoomId(room.getId());
    p2.setUserId(userB);
    p2.setRole("DOCTOR");
    participantRepository.save(p2);

    if (!presenceService.isDoctorOnline(userB)) {
      throw new IllegalStateException("Doctor is offline");
    }

    return room;
  }

  @Transactional
  public ChatParticipant addParticipant(Long roomId, String userId, String role) {
    ChatParticipant p = new ChatParticipant();
    p.setRoomId(roomId);
    p.setUserId(userId);
    p.setRole(role);
    return participantRepository.save(p);
  }

  public List<ChatParticipant> listParticipants(Long roomId) {
    return participantRepository.findByRoomId(roomId);
  }

  public boolean isParticipant(Long roomId, String userId) {
    return participantRepository.existsByRoomIdAndUserId(roomId, userId);
  }

  public Optional<ChatRoom> findRoom(Long roomId) {
    return roomRepository.findById(roomId);
  }

  public List<ChatRoom> findRoomsByUserId(String userId) {
    // get roomIds by participant entries, then fetch room entities
    List<ChatParticipant> parts = participantRepository.findByUserId(userId);
    return parts.stream()
        .map(p -> roomRepository.findById(p.getRoomId()).orElse(null))
        .filter(r -> r != null)
        .toList();
  }
}
