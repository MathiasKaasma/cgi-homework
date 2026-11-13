package com.example.homework.blacklist;

public class BlacklistEntryNotFoundException extends RuntimeException {

  public BlacklistEntryNotFoundException(long id) {
    super("Blacklist entry " + id + " not found");
  }
}
