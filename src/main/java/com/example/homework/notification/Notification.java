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

  private int failureCount;

  private int dispatchCount;

  private String blocked;

  private String detail;

  private Instant createdAt;

  private Instant updatedAt;

  protected Notification() {
  }

  public Notification(NotificationType type, String recipient, String message, int failureCount, int dispatchCount) {
    this.type = type;
    this.recipient = recipient;
    this.message = message;
    this.status = NotificationStatus.PENDING;
    this.failureCount = failureCount;
    this.dispatchCount = dispatchCount;
    this.blocked = "N";
    this.detail = "START";
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void markSent() {
    this.status = NotificationStatus.SENT;
    this.failureCount = 0;
    this.detail = "SENT";
    this.updatedAt = Instant.now();
  }

  public void markFailed(String error, int failureCount) {
    this.status = NotificationStatus.FAILED;
    this.failureCount = failureCount;
    this.detail = truncate(error);
    this.updatedAt = Instant.now();
  }

  public void markRejected(String reason) {
    this.status = NotificationStatus.REJECTED;
    this.detail = truncate(reason);
    this.updatedAt = Instant.now();
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

  public int getFailureCount() {
    return failureCount;
  }

  public int getDispatchCount() {
    return dispatchCount;
  }

  public String getBlocked() {
    return blocked;
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
