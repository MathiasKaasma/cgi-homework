package com.example.homework.notification.sender;

import java.util.regex.Pattern;

import com.example.homework.notification.NotificationType;

import org.springframework.stereotype.Component;

@Component
public class SmsSender implements NotificationSender {

  private static final Pattern PHONE = Pattern.compile("^\\+\\d{7,15}$");

  @Override
  public NotificationType type() {
    return NotificationType.SMS;
  }

  @Override
  public void send(String target, String payload) {
    System.out.println("[SMS] " + target + " -> " + payload);
  }

  @Override
  public boolean isValidRecipient(String target) {
    return PHONE.matcher(target).matches();
  }
}
