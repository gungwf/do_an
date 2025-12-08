package com.service.sys_srv.chat.socket;

import com.service.sys_srv.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * Handshake interceptor robust: không để exception làm 500,
 * luôn catch mọi lỗi, log chi tiết và trả false để từ chối handshake.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtService jwtService;

  public JwtHandshakeInterceptor(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

    String token = null;
    try {
      // 1. header Authorization
      List<String> auth = request.getHeaders().get("Authorization");
      if (auth != null && !auth.isEmpty()) {
        token = auth.get(0);
        if (token.startsWith("Bearer ")) token = token.substring(7);
      }

      // 2. fallback access_token param (SockJS)
      if ((token == null || token.isEmpty()) && request instanceof ServletServerHttpRequest) {
        HttpServletRequest servletReq = ((ServletServerHttpRequest) request).getServletRequest();
        token = servletReq.getParameter("access_token");
      }

      if (token == null || token.isEmpty()) {
        return false;
      }

      // Validate token safely
      boolean valid;
      try {
        valid = jwtService.isTokenValid(token);
      } catch (Exception e) {
        // JwtService might throw; catch and log
        return false;
      }

      if (!valid) {
        return false;
      }

      // extract username and userId, catching potential errors
      String username = null;
      try {
        username = jwtService.extractUsername(token);
      } catch (Exception e) {
      }
      attributes.put("username", username);

      String userId = null;
      try {
        userId = jwtService.extractUserId(token);
      } catch (Exception e) {
      }
      if (userId != null) attributes.put("userId", userId);
      else attributes.put("userId", username);

      try {
        attributes.put("roles", jwtService.extractAuthorities(token));
      } catch (Exception e) {
      }

      return true;

    } catch (Exception ex) {
      // Catch any unexpected exception to avoid 500
      return false;
    }
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {
    // nothing
  }
}
