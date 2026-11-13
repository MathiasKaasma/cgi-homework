package com.example.homework.blacklist;

public class AlreadyBlacklistedException extends RuntimeException {

  public AlreadyBlacklistedException(String recipient) {
    super("Recipient '" + recipient + "' is already blacklisted");
  }
}
