package com.example.homework.notification.sender;

import org.springframework.stereotype.Component;

@Component
public class PushSender implements NotificationSender {

  @Override
  public String type() {
    return "PUSH";
  }

  @Override
  public void send(String target, String payload) {
    System.out.println("[PUSH] " + target + " -> " + payload);
  }
}
