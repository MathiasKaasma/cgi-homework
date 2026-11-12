package com.example.homework.notification;

import jakarta.validation.constraints.NotNull;

public record SendNotificationRequest(@NotNull NotificationType type, String recipient, String message) {
}
