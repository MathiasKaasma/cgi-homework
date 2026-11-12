package com.example.homework.notification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findTop20ByOrderByIdDesc();

  Optional<Notification> findTopByOrderByIdDesc();

  long countByRecipientAndStatusInAndCreatedAtGreaterThanEqual(
      String recipient, Collection<NotificationStatus> statuses, Instant since);

  @Query("select coalesce(max(n.dispatchCount), 0) from Notification n")
  int findMaxDispatchCount();

  List<Notification> findTop3ByTypeAndStatusInOrderByIdDesc(NotificationType type, Collection<NotificationStatus> statuses);
}
