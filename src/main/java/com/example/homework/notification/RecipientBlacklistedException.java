package com.example.homework.notification;

public class RecipientBlacklistedException extends RuntimeException {

  public RecipientBlacklistedException(String recipient) {
    super("Recipient '" + recipient + "' is blacklisted");
  }
}
