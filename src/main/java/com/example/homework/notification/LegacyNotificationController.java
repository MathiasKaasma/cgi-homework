package com.example.homework.notification;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/legacy/notifications")
public class LegacyNotificationController {

  private final LegacyNotificationEngine engine;

  public LegacyNotificationController(LegacyNotificationEngine engine) {
    this.engine = engine;
  }

  @RequestMapping(value = "/send", method = RequestMethod.POST)
  @ResponseBody
  public LegacyNotificationResponse send(@RequestBody LegacyNotificationRequest request) {
    return engine.processNotification(request.type(), request.recipient(), request.message());
  }

  @RequestMapping(value = "/history", method = RequestMethod.GET)
  @ResponseBody
  public List<LegacyNotificationResponse> history() {
    return engine.getHistory();
  }

  @RequestMapping(value = "/latest", method = RequestMethod.GET)
  @ResponseBody
  public LegacyNotificationResponse latest() {
    return engine.getLatestState();
  }
}