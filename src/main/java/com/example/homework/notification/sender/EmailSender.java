package com.example.homework.notification.sender;

import java.util.regex.Pattern;

import com.example.homework.notification.NotificationType;

import org.springframework.stereotype.Component;

@Component
public class EmailSender implements NotificationSender {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  @Override
  public NotificationType type() {
    return NotificationType.EMAIL;
  }

  @Override
  public void send(String recipient, String message) {
    System.out.println("[EMAIL] " + recipient + " -> " + message);
  }

  @Override
  public boolean isValidRecipient(String recipient) {
    return EMAIL.matcher(recipient).matches();
  }
}
