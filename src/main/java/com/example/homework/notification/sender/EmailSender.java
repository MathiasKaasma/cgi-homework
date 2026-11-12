package com.example.homework.notification.sender;

import org.springframework.stereotype.Component;

@Component
public class EmailSender implements NotificationSender {

  @Override
  public String type() {
    return "EMAIL";
  }

  @Override
  public void send(String target, String payload) {
    System.out.println("[EMAIL] " + target + " -> " + payload);
  }
}
