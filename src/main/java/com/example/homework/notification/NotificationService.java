package com.example.homework.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.homework.blacklist.BlacklistService;
import com.example.homework.notification.sender.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private static final int RATE_LIMIT = 3;
  private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
  private static final int CIRCUIT_FAILURE_THRESHOLD = 3;
  private static final Duration CIRCUIT_COOLDOWN = Duration.ofSeconds(30);
  private static final List<NotificationStatus> ATTEMPT_STATUSES =
      List.of(NotificationStatus.SENT, NotificationStatus.FAILED);

  private final Map<NotificationType, NotificationSender> sendersByType;

  private final NotificationRepository repository;

  private final BlacklistService blacklistService;

  private final Clock clock;

  public NotificationService(
      NotificationRepository repository,
      BlacklistService blacklistService,
      List<NotificationSender> senders,
      Clock clock) {
    this.repository = repository;
    this.blacklistService = blacklistService;
    this.clock = clock;

    this.sendersByType = senders.stream()
        .collect(Collectors.toMap(NotificationSender::type, Function.identity()));
  }

  public NotificationResponse send(NotificationType type, String rawRecipient, String rawMessage) {
    String recipient = rawRecipient == null ? "" : rawRecipient.trim();
    String message = rawMessage == null ? "" : rawMessage;
    validateRecipient(type, recipient);

    return dispatch(new Notification(type, recipient, message, clock.instant()));
  }

  public NotificationResponse update(long id, String rawRecipient, String rawMessage) {
    Notification notification = repository.findById(id)
        .orElseThrow(() -> new NotificationNotFoundException(id));
    if (!notification.canBeRetried()) {
      throw new NotificationNotUpdatableException(id, notification.getStatus());
    }

    String recipient = rawRecipient == null ? notification.getRecipient() : rawRecipient.trim();
    String message = rawMessage == null ? notification.getMessage() : rawMessage;
    validateRecipient(notification.getType(), recipient);

    notification.retryWith(recipient, message, clock.instant());
    return dispatch(notification);
  }

  public List<NotificationResponse> getNotifications() {
    return repository.findTop20ByOrderByIdDesc().stream()
        .map(NotificationResponse::from)
        .toList();
  }

  public NotificationResponse getNotification(long id) {
    return repository.findById(id)
        .map(NotificationResponse::from)
        .orElseThrow(() -> new NotificationNotFoundException(id));
  }

  private void validateRecipient(NotificationType type, String recipient) {
    if (recipient.isEmpty() || !sendersByType.get(type).isValidRecipient(recipient)) {
      throw new InvalidRecipientException(type, recipient);
    }
  }

  private NotificationResponse dispatch(Notification notification) {
    NotificationType type = notification.getType();
    String recipient = notification.getRecipient();
    Instant now = notification.getUpdatedAt();

    Optional<RuntimeException> rejection = checkSafeguards(type, recipient, now);
    if (rejection.isPresent()) {
      log.info("Rejected {} notification: {}", type, rejection.get().getMessage());
      notification.markRejected(rejection.get().getMessage(), now);
      repository.save(notification);
      throw rejection.get();
    }

    repository.save(notification);

    try {
      sendersByType.get(type).send(recipient, notification.getMessage());
      notification.markSent(clock.instant());
      return NotificationResponse.from(repository.save(notification));
    } catch (RuntimeException e) {
      log.warn("{} delivery to {} failed", type, recipient, e);
      notification.markFailed(e.getMessage(), clock.instant());
      repository.save(notification);
      throw new ChannelDeliveryException(type, e);
    }
  }

  private Optional<RuntimeException> checkSafeguards(NotificationType type, String recipient, Instant now) {
    if (blacklistService.isBlacklisted(recipient)) {
      return Optional.of(new RecipientBlacklistedException(recipient));
    }
    if (isRateLimited(recipient, now)) {
      return Optional.of(new RateLimitExceededException(recipient, RATE_LIMIT, RATE_LIMIT_WINDOW));
    }
    if (isCircuitOpen(type, now)) {
      return Optional.of(new CircuitOpenException(type));
    }
    return Optional.empty();
  }

  private boolean isRateLimited(String recipient, Instant now) {
    long recentAttempts = repository.countByRecipientAndStatusInAndUpdatedAtGreaterThanEqual(
        recipient, ATTEMPT_STATUSES, now.minus(RATE_LIMIT_WINDOW));
    return recentAttempts >= RATE_LIMIT;
  }

  private boolean isCircuitOpen(NotificationType type, Instant now) {
    List<Notification> lastAttempts = repository.findTop3ByTypeAndStatusInOrderByUpdatedAtDesc(type, ATTEMPT_STATUSES);

    if (lastAttempts.size() < CIRCUIT_FAILURE_THRESHOLD) {
      return false;
    }

    boolean allFailed = lastAttempts.stream()
        .allMatch(attempt -> attempt.getStatus() == NotificationStatus.FAILED);
    Instant lastFailureAt = lastAttempts.get(0).getUpdatedAt();

    return allFailed && lastFailureAt.isAfter(now.minus(CIRCUIT_COOLDOWN));
  }
}
