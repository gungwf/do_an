package com.service.sys_srv.chat.controller;


import com.service.sys_srv.chat.dto.ChatMessageDto;
import com.service.sys_srv.chat.service.ChatService;
import com.service.sys_srv.chat.service.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/api/chat")
public class ChatRestController {


  @Autowired
  private ChatService chatService;


  @Autowired
  private PresenceService presenceService;


  @GetMapping("/rooms/{roomId}/messages")
  public List<ChatMessageDto> getMessages(@PathVariable Long roomId) {
    return chatService.getRecentMessages(roomId, 50);
  }


  @GetMapping("/doctors/online")
  public Set<Long> getOnlineDoctors() {
// Simplified: returns online userIds; in real app filter role=DOCTOR by querying user service or participant table
    return presenceService.getOnlineUserIds();
  }
}