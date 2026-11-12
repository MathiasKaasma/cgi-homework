package com.example.homework.notification;

import java.time.Duration;

public class RateLimitExceededException extends RuntimeException {

  public RateLimitExceededException(String recipient, int limit, Duration window) {
    super("Rate limit exceeded for '" + recipient + "': max " + limit + " per " + window.toSeconds() + "s");
  }
}
