package com.example.homework.notification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findTop20ByOrderByIdDesc();

  long countByRecipientAndStatusInAndCreatedAtGreaterThanEqual(
      String recipient, Collection<NotificationStatus> statuses, Instant since);

  List<Notification> findTop3ByTypeAndStatusInOrderByIdDesc(NotificationType type, Collection<NotificationStatus> statuses);
}
