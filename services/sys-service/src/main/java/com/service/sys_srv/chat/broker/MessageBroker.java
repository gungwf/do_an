package com.service.sys_srv.chat.broker;

public interface MessageBroker {
  void publish(String topic, String messageJson);
}