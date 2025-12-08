package com.service.sys_srv.chat.socket;


import java.util.Arrays;


public class JwtUtil {
  // TODO: replace with your actual JWT parsing/validation logic
  public static JwtUser parseToken(String token) {
// This is a stub for demo. In production call your auth service or JWT library
// Example: decode token, validate signature, extract user id and username.
// For now, fake a user if token equals "demo-user-1" etc.
    if (token == null) return null;
    if (token.startsWith("demo-")) {
// demo-<id>-<name>
      String[] parts = token.split("-");
      Long id = 1L;
      String name = token;
      try { id = Long.valueOf(parts[1]); name = parts.length>2?parts[2]:name; } catch(Exception ignored){}
      return new JwtUser(id, name, Arrays.asList("ROLE_USER"));
    }
// Real impl: return null or parsed user
    return null;
  }
}