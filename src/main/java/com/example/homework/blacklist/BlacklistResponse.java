package com.example.homework.blacklist;

import java.time.Instant;

public record BlacklistResponse(long id, String recipient, String reason, Instant createdAt) {

  public static BlacklistResponse from(BlacklistedRecipient entry) {
    return new BlacklistResponse(entry.getId(), entry.getRecipient(), entry.getReason(), entry.getCreatedAt());
  }
}
