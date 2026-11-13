package com.example.homework.notification;

public class NotificationNotUpdatableException extends RuntimeException {

  public NotificationNotUpdatableException(long id, NotificationStatus status) {
    super("Notification " + id + " is " + status + ", only FAILED or REJECTED notifications can be updated");
  }
}
