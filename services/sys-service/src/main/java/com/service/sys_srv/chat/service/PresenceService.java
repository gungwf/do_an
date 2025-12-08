package com.service.sys_srv.chat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PresenceService {
  private final StringRedisTemplate redis;
  private final String KEY = "chat:online:doctors"; // hash: userId -> iso timestamp or sessionCount

  public PresenceService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  // mark online: we keep a count of active sessions per user to support multi-device
  public void markDoctorOnline(String userId, String sessionId) {
    // store a set per user session: chat:online:doctor:{userId} -> set(sessionId)
    redis.opsForSet().add(keyUser(userId), sessionId);
    // set last seen timestamp in global hash for listing
    redis.opsForHash().put(KEY, userId, Instant.now().toString());
    // optional: set TTL to auto-expire if set is empty - but we manage removal on disconnect
  }

  public void markDoctorOffline(String userId, String sessionId) {
    String userKey = keyUser(userId);
    redis.opsForSet().remove(userKey, sessionId);
    Long size = redis.opsForSet().size(userKey);
    if (size == null || size == 0) {
      // remove global hash entry
      redis.opsForHash().delete(KEY, userId);
      // also delete the per-user set
      redis.delete(userKey);
    } else {
      // still has sessions; update timestamp
      redis.opsForHash().put(KEY, userId, Instant.now().toString());
    }
  }

  public boolean isDoctorOnline(String userId) {
    return redis.opsForHash().hasKey(KEY, userId);
  }

  // return set of online doctor ids
  public Set<String> listOnlineDoctors() {
    Map<Object, Object> map = redis.opsForHash().entries(KEY);
    return map.keySet().stream().map(Object::toString).collect(Collectors.toSet());
  }

  private String keyUser(String userId) {
    return "chat:online:doctor:" + userId + ":sessions";
  }
}
