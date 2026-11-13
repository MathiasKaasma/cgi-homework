package com.example.homework.notification.sender;

import java.util.regex.Pattern;

import com.example.homework.notification.NotificationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsSender implements NotificationSender {

  private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

  private static final Pattern PHONE = Pattern.compile("^\\+\\d{7,15}$");

  @Override
  public NotificationType type() {
    return NotificationType.SMS;
  }

  @Override
  public void send(String recipient, String message) {
    log.info("Sending {} to {} ({} chars)", type(), recipient, message.length());
  }

  @Override
  public boolean isValidRecipient(String recipient) {
    return PHONE.matcher(recipient).matches();
  }
}
