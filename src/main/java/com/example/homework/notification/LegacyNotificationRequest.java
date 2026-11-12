package com.example.homework.notification;

public record LegacyNotificationRequest(NotificationType type, String target, String payload) {
}
