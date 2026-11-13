package com.example.homework.notification.sender;

import java.util.regex.Pattern;

import com.example.homework.notification.NotificationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailSender implements NotificationSender {

  private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  @Override
  public NotificationType type() {
    return NotificationType.EMAIL;
  }

  @Override
  public void send(String recipient, String message) {
    log.info("Sending {} to {} ({} chars)", type(), recipient, message.length());
  }

  @Override
  public boolean isValidRecipient(String recipient) {
    return EMAIL.matcher(recipient).matches();
  }
}
