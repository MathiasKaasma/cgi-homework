package com.example.homework.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.homework.notification.sender.NotificationSender;
import org.springframework.stereotype.Service;

@Service
public class LegacyNotificationEngine {

  private static final int RATE_LIMIT = 3;
  private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
  private static final int CIRCUIT_FAILURE_THRESHOLD = 3;
  private static final Duration CIRCUIT_COOLDOWN = Duration.ofSeconds(30);
  private static final List<NotificationStatus> ATTEMPT_STATUSES =
      List.of(NotificationStatus.SENT, NotificationStatus.FAILED);

  private final Map<NotificationType, NotificationSender> sendersByType;

  private final NotificationRepository repository;

  private final Clock clock;

  public LegacyNotificationEngine(NotificationRepository repository, List<NotificationSender> senders, Clock clock) {
    this.repository = repository;
    this.clock = clock;

    this.sendersByType = senders.stream()
        .collect(Collectors.toMap(NotificationSender::type, Function.identity()));
  }

  public synchronized LegacyNotificationResponse processNotification(NotificationType type, String target, String payload)
      throws Exception {

    Instant now = clock.instant();
    String safeTarget = target != null ? target.trim() : null;
    String safePayload = payload != null ? payload : "";
    NotificationSender sender = sendersByType.get(type);

    if (safeTarget == null || safeTarget.isEmpty() || !sender.isValidRecipient(safeTarget)) {
      throw new InvalidRecipientException(type, safeTarget);
    }

    if (isUserBlacklistedOrRateLimited(safeTarget, now)) {
      throw new RuntimeException("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");
    }

    int dispatchCount = repository.findMaxDispatchCount() + 1;
    Notification notification = repository.save(
        new Notification(type, safeTarget, safePayload, dispatchCount, now));

    if (isCircuitOpen(type, now)) {
      notification.markRejected("Circuit breaker OPEN via database state.", now);
      repository.save(notification);
      throw new RuntimeException("Circuit breaker OPEN via database state.");
    }

    try {
      sender.send(safeTarget, safePayload);
      notification.markSent(clock.instant());
      return LegacyNotificationResponse.from(repository.save(notification));
    } catch (Exception e) {
      notification.markFailed(e.getMessage(), clock.instant());
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

  private boolean isUserBlacklistedOrRateLimited(String target, Instant now) {
    if (target.toLowerCase().contains("blocked")) {
      return true;
    }

    long recentCount = repository.countByRecipientAndCreatedAtGreaterThanEqual(target, now.minus(RATE_LIMIT_WINDOW));

    return recentCount >= RATE_LIMIT;
  }

  private boolean isCircuitOpen(NotificationType type, Instant now) {
    List<Notification> lastAttempts = repository.findTop3ByTypeAndStatusInOrderByIdDesc(type, ATTEMPT_STATUSES);

    if (lastAttempts.size() < CIRCUIT_FAILURE_THRESHOLD) {
      return false;
    }

    boolean allFailed = lastAttempts.stream()
        .allMatch(attempt -> attempt.getStatus() == NotificationStatus.FAILED);
    Instant lastFailureAt = lastAttempts.get(0).getUpdatedAt();

    return allFailed && lastFailureAt.isAfter(now.minus(CIRCUIT_COOLDOWN));
  }
}
