package com.service.sys_srv.chat.broker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMessageBrokerImpl implements MessageBroker {
  private final StringRedisTemplate redisTemplate;


  @Autowired
  public RedisMessageBrokerImpl(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void publish(String topic, String messageJson) {
    redisTemplate.convertAndSend(topic, messageJson);
  }
}