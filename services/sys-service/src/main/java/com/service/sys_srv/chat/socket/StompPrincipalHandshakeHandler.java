package com.service.sys_srv.chat.socket;

import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class StompPrincipalHandshakeHandler extends DefaultHandshakeHandler {
  @Override
  protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
      org.springframework.web.socket.WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    Object userIdAttr = attributes.get("userId");
    Object username = attributes.get("username");
    final String principalName;
    if (userIdAttr != null) {
      principalName = "user-" + String.valueOf(userIdAttr);
    } else if (username != null) {
      principalName = "user-" + username.toString();
    } else {
      principalName = "anonymous";
    }
    return new Principal() {
      @Override
      public String getName() {
        return principalName;
      }
    };
  }
}
