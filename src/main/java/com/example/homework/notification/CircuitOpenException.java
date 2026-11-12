package com.example.homework.notification;

public class CircuitOpenException extends RuntimeException {

  public CircuitOpenException(NotificationType type) {
    super(type + " channel is temporarily unavailable after repeated failures");
  }
}
