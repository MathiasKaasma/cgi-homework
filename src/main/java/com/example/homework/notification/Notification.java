package com.example.homework.notification;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification")
public class Notification {

  private static final int MAX_DETAIL_LENGTH = 255;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  private NotificationType type;

  private String recipient;

  @Lob
  private String message;

  @Enumerated(EnumType.STRING)
  private NotificationStatus status;

  private String detail;

  private Instant createdAt;

  private Instant updatedAt;

  protected Notification() {
  }

  public Notification(NotificationType type, String recipient, String message, Instant now) {
    this.type = type;
    this.recipient = recipient;
    this.message = message;
    this.status = NotificationStatus.PENDING;
    this.detail = "START";
    this.createdAt = now;
    this.updatedAt = this.createdAt;
  }

  public void markSent(Instant now) {
    this.status = NotificationStatus.SENT;
    this.detail = "SENT";
    this.updatedAt = now;
  }

  public void markFailed(String error, Instant now) {
    this.status = NotificationStatus.FAILED;
    this.detail = truncate(error);
    this.updatedAt = now;
  }

  public void markRejected(String reason, Instant now) {
    this.status = NotificationStatus.REJECTED;
    this.detail = truncate(reason);
    this.updatedAt = now;
  }

  private static String truncate(String value) {
    if (value == null || value.length() <= MAX_DETAIL_LENGTH) {
      return value;
    }
    return value.substring(0, MAX_DETAIL_LENGTH);
  }

  public Long getId() {
    return id;
  }

  public NotificationType getType() {
    return type;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getMessage() {
    return message;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public String getDetail() {
    return detail;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
