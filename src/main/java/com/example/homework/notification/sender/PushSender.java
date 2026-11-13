package com.example.homework.notification.sender;

import com.example.homework.notification.NotificationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushSender implements NotificationSender {

  private static final Logger log = LoggerFactory.getLogger(PushSender.class);

  @Override
  public NotificationType type() {
    return NotificationType.PUSH;
  }

  @Override
  public void send(String recipient, String message) {
    log.info("Sending {} to {} ({} chars)", type(), recipient, message.length());
  }

  @Override
  public boolean isValidRecipient(String recipient) {
    return !recipient.isBlank();
  }
}
