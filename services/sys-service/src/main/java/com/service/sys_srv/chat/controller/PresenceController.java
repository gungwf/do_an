package com.service.sys_srv.chat.controller;

import com.service.sys_srv.chat.service.PresenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/chat")
public class PresenceController {
  private final PresenceService presence;

  public PresenceController(PresenceService presence) {
    this.presence = presence;
  }

  @GetMapping("/doctors/online")
  public Set<String> listOnlineDoctors() {
    return presence.listOnlineDoctors();
  }
}
