package com.example.homework.notification;

import java.sql.Timestamp;

public record LegacyNotificationResponse(
    long id,
    String type,
    String target,
    String payload,
    String status,
    int failureCount,
    int dispatchCount,
    String blocked,
    String message,
    Timestamp createdAt,
    Timestamp updatedAt) {
}
