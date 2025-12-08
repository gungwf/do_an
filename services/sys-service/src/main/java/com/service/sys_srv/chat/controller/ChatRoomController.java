package com.service.sys_srv.chat.controller;

import com.service.sys_srv.chat.dto.ChatMessageDto;
import com.service.sys_srv.chat.entity.ChatParticipant;
import com.service.sys_srv.chat.entity.ChatRoom;
import com.service.sys_srv.chat.service.ChatRoomService;
import com.service.sys_srv.chat.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

  private final ChatRoomService chatRoomService;
  private final ChatService chatService;

  public ChatRoomController(ChatRoomService chatRoomService, ChatService chatService) {
    this.chatRoomService = chatRoomService;
    this.chatService = chatService;
  }

  /**
   * Tạo room 1-1 giữa userA và userB.
   * Query params: ?userA=<id>&userB=<id>
   * Trả về ChatRoom JSON.
   */
  @PostMapping("/one2one")
  public ResponseEntity<ChatRoom> createOneToOne(@RequestParam String userA, @RequestParam String userB) {
    ChatRoom room = chatRoomService.createOneToOne(userA, userB);
    return ResponseEntity.ok(room);
  }

  /**
   * Thêm participant vào room (admin/logic gọi).
   * Body: role optional (PATIENT/DOCTOR)
   */
  @PostMapping("/{roomId}/participants")
  public ResponseEntity<ChatParticipant> addParticipant(
      @PathVariable Long roomId,
      @RequestParam String userId,
      @RequestParam(defaultValue = "PATIENT") String role) {
    ChatParticipant p = chatRoomService.addParticipant(roomId, userId, role);
    return ResponseEntity.ok(p);
  }

  @GetMapping("/{roomId}/participants")
  public ResponseEntity<List<ChatParticipant>> listParticipants(@PathVariable Long roomId) {
    return ResponseEntity.ok(chatRoomService.listParticipants(roomId));
  }

  @GetMapping("/by-user/{userId}")
  public ResponseEntity<List<ChatRoom>> listRoomsForUser(@PathVariable String userId) {
    return ResponseEntity.ok(chatRoomService.findRoomsByUserId(userId));
  }

  @GetMapping("/{roomId}/messages")
  public ResponseEntity<List<ChatMessageDto>> getMessages(
      @PathVariable Long roomId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    // Inject ChatService to get messages
    return ResponseEntity.ok(chatService.getMessagesByRoom(roomId, page, size));
  }
}
