package com.example.homework.notification;

public class InvalidRecipientException extends RuntimeException {

  public InvalidRecipientException(NotificationType type, String target) {
    super(target == null || target.isBlank()
        ? type + " recipient is required"
        : "'" + target + "' is not a valid " + type + " recipient");
  }
}
