package com.example.homework.notification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.homework.notification.sender.NotificationSender;
import org.springframework.stereotype.Service;

@Service
public class LegacyNotificationEngine {

  private final Map<NotificationType, NotificationSender> sendersByType;

  private final NotificationRepository repository;

  public LegacyNotificationEngine(NotificationRepository repository, List<NotificationSender> senders) {
    this.repository = repository;

    this.sendersByType = senders.stream()
        .collect(Collectors.toMap(NotificationSender::type, Function.identity()));
  }

  public synchronized LegacyNotificationResponse processNotification(NotificationType type, String target, String payload)
      throws Exception {

    String safeTarget = target != null ? target.trim() : null;
    String safePayload = payload != null ? payload : "";
    NotificationSender sender = sendersByType.get(type);

    if (safeTarget == null || safeTarget.isEmpty() || !sender.isValidRecipient(safeTarget)) {
      throw new InvalidRecipientException(type, safeTarget);
    }

    if (isUserBlacklistedOrRateLimited(safeTarget)) {
      throw new RuntimeException("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");
    }

    int failureCount = currentFailureCount();
    int dispatchCount = repository.findMaxDispatchCount() + 1;
    Notification notification = repository.save(
        new Notification(type, safeTarget, safePayload, failureCount, dispatchCount));

    if (failureCount >= 3) {
      notification.markRejected("Circuit breaker OPEN via database state.");
      repository.save(notification);
      throw new RuntimeException("Circuit breaker OPEN via database state.");
    }

    try {
      sender.send(safeTarget, safePayload);
      notification.markSent();
      return LegacyNotificationResponse.from(repository.save(notification));
    } catch (Exception e) {
      notification.markFailed(e.getMessage(), failureCount + 1);
      repository.save(notification);
      throw e;
    }
  }

  public List<LegacyNotificationResponse> getHistory() {
    return repository.findTop20ByOrderByIdDesc().stream()
        .map(LegacyNotificationResponse::from)
        .toList();
  }

  public LegacyNotificationResponse getLatestState() {
    return repository.findTopByOrderByIdDesc()
        .map(LegacyNotificationResponse::from)
        .orElse(null);
  }

  private boolean isUserBlacklistedOrRateLimited(String target) {
    if (target.toLowerCase().contains("blocked")) {
      return true;
    }

    long recentCount = repository.countByRecipientAndCreatedAtGreaterThanEqual(target, Instant.now().minusSeconds(60));

    return recentCount >= 3;
  }

  private int currentFailureCount() {
    return repository.findTopByOrderByIdDesc()
        .map(Notification::getFailureCount)
        .orElse(0);
  }
}
