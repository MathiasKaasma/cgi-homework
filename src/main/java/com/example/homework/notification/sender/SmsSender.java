package com.example.homework.notification.sender;

import com.example.homework.notification.NotificationType;

import org.springframework.stereotype.Component;

@Component
public class SmsSender implements NotificationSender {

  @Override
  public NotificationType type() {
    return NotificationType.SMS;
  }

  @Override
  public void send(String target, String payload) {
    System.out.println("[SMS] " + target + " -> " + payload);
  }
}
