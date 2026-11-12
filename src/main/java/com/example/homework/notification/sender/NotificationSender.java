package com.example.homework.notification.sender;

import com.example.homework.notification.NotificationType;

public interface NotificationSender {

  NotificationType type();

  void send(String target, String payload);
}
