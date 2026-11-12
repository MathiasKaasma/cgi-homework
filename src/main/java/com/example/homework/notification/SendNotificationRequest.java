package com.example.homework.notification;

public record SendNotificationRequest(NotificationType type, String recipient, String message) {
}
