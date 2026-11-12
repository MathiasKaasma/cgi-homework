package com.example.homework.notification;

import java.time.Instant;

public record LegacyNotificationResponse(
    long id,
    NotificationType type,
    String target,
    String payload,
    NotificationStatus status,
    int failureCount,
    int dispatchCount,
    String blocked,
    String message,
    Instant createdAt,
    Instant updatedAt) {

  public static LegacyNotificationResponse from(Notification notification) {
    return new LegacyNotificationResponse(
        notification.getId(),
        notification.getType(),
        notification.getRecipient(),
        notification.getMessage(),
        notification.getStatus(),
        notification.getFailureCount(),
        notification.getDispatchCount(),
        notification.getBlocked(),
        notification.getDetail(),
        notification.getCreatedAt(),
        notification.getUpdatedAt());
  }
}
