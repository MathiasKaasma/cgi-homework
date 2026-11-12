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
  public void send(String recipient, String message) {
    System.out.println("[PUSH] " + recipient + " -> " + message);
  }

  @Override
  public boolean isValidRecipient(String recipient) {
    return !recipient.isBlank();
  }
}
