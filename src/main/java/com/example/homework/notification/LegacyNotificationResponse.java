package com.example.homework.notification;

import java.time.Instant;

public record LegacyNotificationResponse(
    long id,
    NotificationType type,
    String recipient,
    String message,
    NotificationStatus status,
    String detail,
    Instant createdAt,
    Instant updatedAt) {

  public static LegacyNotificationResponse from(Notification notification) {
    return new LegacyNotificationResponse(
        notification.getId(),
        notification.getType(),
        notification.getRecipient(),
        notification.getMessage(),
        notification.getStatus(),
        notification.getDetail(),
        notification.getCreatedAt(),
        notification.getUpdatedAt());
  }
}
