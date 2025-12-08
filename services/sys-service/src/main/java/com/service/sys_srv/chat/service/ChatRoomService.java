package com.service.sys_srv.chat.service;

import com.service.sys_srv.chat.entity.ChatParticipant;
import com.service.sys_srv.chat.entity.ChatRoom;
import com.service.sys_srv.chat.repository.ChatParticipantRepository;
import com.service.sys_srv.chat.repository.ChatRoomRepository;
import com.service.sys_srv.entity.User;
import com.service.sys_srv.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatRoomService {

  private final ChatRoomRepository roomRepository;
  private final ChatParticipantRepository participantRepository;
  private final PresenceService presenceService;
  private final UserRepository userRepository;

  public ChatRoomService(ChatRoomRepository roomRepository, ChatParticipantRepository participantRepository,
      PresenceService presenceService, UserRepository userRepository) {
    this.roomRepository = roomRepository;
    this.participantRepository = participantRepository;
    this.presenceService = presenceService;
    this.userRepository = userRepository;
  }

  /**
   * Load user names for participants
   */
  private void loadUserNames(List<ChatParticipant> participants) {
    for (ChatParticipant p : participants) {
      try {
        UUID userId = UUID.fromString(p.getUserId());
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
          p.setUserName(userOpt.get().getFullName());
        } else {
          p.setUserName("Unknown User");
        }
      } catch (Exception e) {
        p.setUserName("Unknown User");
      }
    }
  }

  @Transactional
  public ChatRoom createOneToOne(String userA, String userB) {
    // Check if room already exists between these 2 users
    List<ChatParticipant> userARooms = participantRepository.findByUserId(userA);
    for (ChatParticipant p : userARooms) {
      List<ChatParticipant> roomParticipants = participantRepository.findByRoomId(p.getRoomId());
      // Check if userB is also in this room
      boolean hasBothUsers = roomParticipants.stream()
          .anyMatch(rp -> rp.getUserId().equals(userB));
      if (hasBothUsers && roomParticipants.size() == 2) {
        // Room exists, return it
        ChatRoom existingRoom = roomRepository.findById(p.getRoomId()).orElse(null);
        if (existingRoom != null) {
          loadUserNames(roomParticipants);
          existingRoom.setParticipants(roomParticipants);
          return existingRoom;
        }
      }
    }

    // Room doesn't exist, create new one
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

    // Load participants before returning
    List<ChatParticipant> newParticipants = List.of(p1, p2);
    loadUserNames(newParticipants);
    room.setParticipants(newParticipants);

//    if (!presenceService.isDoctorOnline(userB)) {
//      throw new IllegalStateException("Doctor is offline");
//    }

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
        .map(p -> {
          ChatRoom room = roomRepository.findById(p.getRoomId()).orElse(null);
          if (room != null) {
            // Load participants for this room
            List<ChatParticipant> roomParticipants = participantRepository.findByRoomId(room.getId());
            loadUserNames(roomParticipants);
            room.setParticipants(roomParticipants);
          }
          return room;
        })
        .filter(r -> r != null)
        .toList();
  }
}
