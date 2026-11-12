package com.example.homework.notification.sender;

import com.example.homework.notification.NotificationType;

import org.springframework.stereotype.Component;

@Component
public class EmailSender implements NotificationSender {

  @Override
  public NotificationType type() {
    return NotificationType.EMAIL;
  }

  @Override
  public void send(String target, String payload) {
    System.out.println("[EMAIL] " + target + " -> " + payload);
  }
}
