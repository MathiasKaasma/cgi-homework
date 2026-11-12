package com.example.homework.notification.sender;

import com.example.homework.notification.NotificationType;

import org.springframework.stereotype.Component;

@Component
public class PushSender implements NotificationSender {

  @Override
  public NotificationType type() {
    return NotificationType.PUSH;
  }

  @Override
  public void send(String target, String payload) {
    System.out.println("[PUSH] " + target + " -> " + payload);
  }

  @Override
  public boolean isValidRecipient(String target) {
    return !target.isBlank();
  }
}
