package com.service.sys_srv.chat.socket;

import com.service.sys_srv.chat.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class StompEventListener {
  private static final Logger log = LoggerFactory.getLogger(StompEventListener.class);
  private final PresenceService presenceService;

  public StompEventListener(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  @EventListener
  public void handleSessionConnected(SessionConnectedEvent event) {
    StompHeaderAccessor sh = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = sh.getSessionId();
    // sessionAttributes are set in JwtHandshakeInterceptor on handshake
    Object userIdObj = sh.getSessionAttributes() != null ? sh.getSessionAttributes().get("userId") : null;
    Object rolesObj = sh.getSessionAttributes() != null ? sh.getSessionAttributes().get("roles") : null;

    if (userIdObj != null && rolesObj != null) {
      String userId = userIdObj.toString();
      @SuppressWarnings("unchecked")
      java.util.List<String> roles = (java.util.List<String>) rolesObj;
      if (roles.contains("DOCTOR")) {
        presenceService.markDoctorOnline(userId, sessionId);
        log.info("Doctor {} connected (session {}) -> marked online", userId, sessionId);
      }
    }
  }

  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor sh = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = sh.getSessionId();
    Object userIdObj = sh.getSessionAttributes() != null ? sh.getSessionAttributes().get("userId") : null;
    Object rolesObj = sh.getSessionAttributes() != null ? sh.getSessionAttributes().get("roles") : null;
    if (userIdObj != null && rolesObj != null) {
      String userId = userIdObj.toString();
      @SuppressWarnings("unchecked")
      java.util.List<String> roles = (java.util.List<String>) rolesObj;
      if (roles.contains("DOCTOR")) {
        presenceService.markDoctorOffline(userId, sessionId);
        log.info("Doctor {} disconnected (session {}) -> maybe offline", userId, sessionId);
      }
    }
  }
}
