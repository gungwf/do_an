package com.service.sys_srv.chat.service;

import com.service.sys_srv.chat.dto.ChatMessageDto;
import com.service.sys_srv.chat.entity.ChatMessage;
import com.service.sys_srv.chat.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

  private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

  @Autowired
  private ChatMessageRepository messageRepository;

  @Override
  @Transactional
  public ChatMessageDto saveMessage(ChatMessageDto dto) {
    ChatMessage m = new ChatMessage();
    m.setRoomId(dto.getRoomId());
    m.setSenderId(dto.getSenderId());
    m.setContent(dto.getContent());
    m.setMessageType(dto.getMessageType() == null ? "TEXT" : dto.getMessageType());
    m.setMetadata(dto.getMetadata());
    ChatMessage saved = messageRepository.save(m);
    ChatMessageDto out = map(saved);
    return out;
  }

  @Override
  public List<ChatMessageDto> getRecentMessages(Long roomId, int limit) {
    return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, limit))
        .stream().map(this::map).collect(Collectors.toList());
  }

  private ChatMessageDto map(ChatMessage m) {
    ChatMessageDto d = new ChatMessageDto();
    d.setId(m.getId());
    d.setRoomId(m.getRoomId());
    d.setSenderId(m.getSenderId());
    d.setContent(m.getContent());
    d.setMessageType(m.getMessageType());
    d.setMetadata(m.getMetadata());
    d.setCreatedAt(m.getCreatedAt());
    // quan trọng: set cả flags để client thấy thay đổi
    d.setDelivered(Boolean.TRUE.equals(m.getDelivered()));
    d.setRead(Boolean.TRUE.equals(m.getRead()));
    return d;
  }

  @Override
  @Transactional
  public ChatMessageDto markDelivered(Long messageId, String deliveredByUserId) {
    log.info("markDelivered called: messageId={} byUser={}", messageId, deliveredByUserId);
    ChatMessage msg = messageRepository.findById(messageId)
        .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
    if (!Boolean.TRUE.equals(msg.getDelivered())) {
      msg.setDelivered(true);
      // optional: msg.setDeliveredAt(Instant.now());
      messageRepository.save(msg);
      log.debug("Message {} marked delivered", messageId);
    } else {
      log.debug("Message {} already delivered", messageId);
    }
    ChatMessageDto dto = map(msg);
    // optionally record who delivered (deliveredBy) in metadata if needed
    return dto;
  }

  @Override
  @Transactional
  public ChatMessageDto markRead(Long messageId, String readByUserId) {
    log.info("markRead called: messageId={} byUser={}", messageId, readByUserId);
    ChatMessage msg = messageRepository.findById(messageId)
        .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
    if (!Boolean.TRUE.equals(msg.getRead())) {
      msg.setRead(true);
      // optional: msg.setReadAt(Instant.now());
      messageRepository.save(msg);
      log.debug("Message {} marked read", messageId);
    } else {
      log.debug("Message {} already read", messageId);
    }
    ChatMessageDto dto = map(msg);
    return dto;
  }
}
