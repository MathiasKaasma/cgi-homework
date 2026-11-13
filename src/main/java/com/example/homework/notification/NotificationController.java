package com.example.homework.notification;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<NotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
    NotificationResponse response = service.send(request.type(), request.recipient(), request.message());
    return ResponseEntity
        .created(URI.create("/api/notifications/" + response.id()))
        .body(response);
  }

  @GetMapping
  public List<NotificationResponse> getNotifications() {
    return service.getNotifications();
  }

  @GetMapping("/{id}")
  public NotificationResponse getNotification(@PathVariable long id) {
    return service.getNotification(id);
  }

  @PatchMapping("/{id}")
  public NotificationResponse update(@PathVariable long id, @RequestBody UpdateNotificationRequest request) {
    return service.update(id, request.recipient(), request.message());
  }
}
