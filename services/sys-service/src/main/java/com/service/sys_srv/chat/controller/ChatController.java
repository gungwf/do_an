package com.service.sys_srv.chat.controller;

import com.service.sys_srv.chat.dto.ChatMessageDto;
import com.service.sys_srv.chat.repository.ChatParticipantRepository;
import com.service.sys_srv.chat.service.ChatRoomService;
import com.service.sys_srv.chat.service.ChatService;
import com.service.sys_srv.chat.broker.MessageBroker;
import com.service.sys_srv.chat.util.JsonUtils;
import com.service.sys_srv.entity.User;
import com.service.sys_srv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class ChatController {

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @Autowired
  private ChatService chatService;

  @Autowired
  private MessageBroker messageBroker;

  @Autowired
  private ChatParticipantRepository chatParticipantRepository;

  @Autowired
  private ChatRoomService chatRoomService;

  @Autowired
  private UserRepository userRepository;

  @MessageMapping("/chat.send")
  public void sendMessage(@Payload ChatMessageDto chatMessage,
      SimpMessageHeaderAccessor headerAccessor,
      Principal principal) {
    // Log received message for debugging
    System.out.println("📨 Received message from client - senderId: " + chatMessage.getSenderId() + ", roomId: " + chatMessage.getRoomId());
    
    // 1) Lấy userId an toàn từ session attributes (đã được JwtHandshakeInterceptor lưu)
    Object userIdAttr = null;
    if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
      userIdAttr = headerAccessor.getSessionAttributes().get("userId");
    }

    String senderIdStr = null;
    if (userIdAttr != null) {
      senderIdStr = userIdAttr.toString();
    } else if (principal != null) {
      // principal.getName() có thể là "user-<id>" theo handshake handler; bóc prefix nếu cần
      String pname = principal.getName();
      if (pname != null) {
        if (pname.startsWith("user-")) senderIdStr = pname.substring("user-".length());
        else senderIdStr = pname;
      }
    }

    System.out.println("🔐 Session userId: " + senderIdStr + ", Principal: " + (principal != null ? principal.getName() : "null"));

    if (senderIdStr == null || senderIdStr.isEmpty()) {
      // Không có sender id — từ chối hoặc log; không để null đi vào DB
      // Bạn có thể ném exception hoặc bỏ qua lưu
      throw new IllegalStateException("Sender not authenticated for websocket session");
    }

    Long roomId = chatMessage.getRoomId();
    if (!chatRoomService.isParticipant(roomId, senderIdStr)) {
      // Tùy chọn: tạo participant tự động hoặc từ chối
      throw new IllegalStateException("User not participant of room");
    }

    // Gán sender vào DTO (dto senderId type cần là String nếu dùng UUID)
    chatMessage.setSenderId(senderIdStr);

    // Load sender name
    try {
      UUID userId = UUID.fromString(senderIdStr);
      Optional<User> userOpt = userRepository.findById(userId);
      if (userOpt.isPresent()) {
        chatMessage.setSenderName(userOpt.get().getFullName());
      } else {
        chatMessage.setSenderName("Unknown User");
      }
    } catch (Exception e) {
      chatMessage.setSenderName("Unknown User");
    }

    // Lưu message
    var saved = chatService.saveMessage(chatMessage);
    
    System.out.println("✅ Saved message - id: " + saved.getId() + ", senderId: " + saved.getSenderId() + ", senderName: " + saved.getSenderName());

    // Gửi tới topic /topic/chat.{roomId}
    messagingTemplate.convertAndSend("/topic/chat." + saved.getRoomId(), saved);

    // publish via broker
    String json = JsonUtils.toJson(saved);
    messageBroker.publish("chat:room:" + saved.getRoomId(), json);
  }

  @MessageMapping("/chat.delivered")
  public void handleDelivered(@Payload Map<String, Object> payload,
      SimpMessageHeaderAccessor headerAccessor) {
    Long messageId = Long.valueOf(payload.get("messageId").toString());
    // Lấy userId an toàn từ session (người xác nhận đã nhận)
    Object uid = headerAccessor.getSessionAttributes().get("userId");
    String userId = uid != null ? uid.toString() : null;

    ChatMessageDto updated = chatService.markDelivered(messageId, userId);

    // Broadcast event tới room để sender/others update UI
    Map<String, Object> event = Map.of(
        "event", "delivered",
        "messageId", messageId,
        "roomId", updated.getRoomId(),
        "updated", updated
    );
    messagingTemplate.convertAndSend("/topic/chat." + updated.getRoomId(), event);
  }

  // 2) Khi client báo đã đọc (UI-level read)
  @MessageMapping("/chat.read")
  public void handleRead(@Payload Map<String, Object> payload,
      SimpMessageHeaderAccessor headerAccessor) {
    Long messageId = Long.valueOf(payload.get("messageId").toString());
    Object uid = headerAccessor.getSessionAttributes().get("userId");
    String userId = uid != null ? uid.toString() : null;

    ChatMessageDto updated = chatService.markRead(messageId, userId);

    Map<String, Object> event = Map.of(
        "event", "read",
        "messageId", messageId,
        "roomId", updated.getRoomId(),
        "updated", updated
    );
    messagingTemplate.convertAndSend("/topic/chat." + updated.getRoomId(), event);
  }
}
