package com.example.homework.notification;

public class NotificationNotFoundException extends RuntimeException {

  public NotificationNotFoundException(long id) {
    super("Notification " + id + " not found");
  }
}
