package com.example.homework.notification;

public class InvalidRecipientException extends RuntimeException {

  public InvalidRecipientException(NotificationType type, String recipient) {
    super(recipient == null || recipient.isBlank()
        ? type + " recipient is required"
        : "'" + recipient + "' is not a valid " + type + " recipient");
  }
}
