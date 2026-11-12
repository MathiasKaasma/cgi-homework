package com.example.homework.notification.sender;

public interface NotificationSender {

  String type();

  void send(String target, String payload);
}
