package com.example.homework.blacklist;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "blacklisted_recipient")
public class BlacklistedRecipient {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String recipient;

  private String reason;

  private Instant createdAt;

  protected BlacklistedRecipient() {
  }

  public BlacklistedRecipient(String recipient, String reason, Instant createdAt) {
    this.recipient = recipient;
    this.reason = reason;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getReason() {
    return reason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
