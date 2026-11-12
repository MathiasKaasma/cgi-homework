package com.example.homework.notification;

public class ChannelDeliveryException extends RuntimeException {

  public ChannelDeliveryException(NotificationType type, Throwable cause) {
    super(type + " delivery failed: " + cause.getMessage(), cause);
  }
}
