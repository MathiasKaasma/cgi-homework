package com.example.homework.notification.sender;

import org.springframework.stereotype.Component;

@Component
public class SmsSender implements NotificationSender {

  @Override
  public String type() {
    return "SMS";
  }

  @Override
  public void send(String target, String payload) {
    System.out.println("[SMS] " + target + " -> " + payload);
  }
}
