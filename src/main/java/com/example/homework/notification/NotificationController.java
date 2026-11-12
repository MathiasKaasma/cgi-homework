package com.example.homework.notification;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/legacy/notifications")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @RequestMapping(value = "/send", method = RequestMethod.POST)
  @ResponseBody
  public NotificationResponse send(@RequestBody SendNotificationRequest request) {
    return service.processNotification(request.type(), request.recipient(), request.message());
  }

  @RequestMapping(value = "/history", method = RequestMethod.GET)
  @ResponseBody
  public List<NotificationResponse> history() {
    return service.getHistory();
  }

  @RequestMapping(value = "/latest", method = RequestMethod.GET)
  @ResponseBody
  public NotificationResponse latest() {
    return service.getLatestState();
  }
}