package com.example.homework.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService service;

  @Test
  void sendReturnsCreatedWithLocation() throws Exception {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    NotificationResponse sent = new NotificationResponse(
        5, NotificationType.EMAIL, "a@b.com", "hi", NotificationStatus.SENT, null, now, now);
    when(service.send(NotificationType.EMAIL, "a@b.com", "hi")).thenReturn(sent);

    postNotification("""
        {"type": "EMAIL", "recipient": "a@b.com", "message": "hi"}
        """)
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/notifications/5"))
        .andExpect(jsonPath("$.status").value("SENT"));
  }

  @Test
  void unknownTypeIsBadRequest() throws Exception {
    postNotification("""
        {"type": "FAX", "recipient": "a@b.com", "message": "hi"}
        """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("type must be one of [EMAIL, PUSH, SMS]"));
  }

  @Test
  void missingTypeIsBadRequest() throws Exception {
    postNotification("""
        {"recipient": "a@b.com", "message": "hi"}
        """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("type must not be null"));
  }

  @Test
  void rateLimitedIsTooManyRequests() throws Exception {
    when(service.send(any(), anyString(), anyString()))
        .thenThrow(new RateLimitExceededException("a@b.com", 3, Duration.ofMinutes(1)));

    postNotification("""
        {"type": "EMAIL", "recipient": "a@b.com", "message": "hi"}
        """)
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void channelFailureIsBadGateway() throws Exception {
    when(service.send(any(), anyString(), anyString()))
        .thenThrow(new ChannelDeliveryException(NotificationType.EMAIL, new RuntimeException("SMTP Timeout")));

    postNotification("""
        {"type": "EMAIL", "recipient": "a@b.com", "message": "hi"}
        """)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value("EMAIL delivery failed: SMTP Timeout"));
  }

  @Test
  void unknownIdIsNotFound() throws Exception {
    when(service.getNotification(999)).thenThrow(new NotificationNotFoundException(999));

    mockMvc.perform(get("/api/notifications/999"))
        .andExpect(status().isNotFound());
  }

  private ResultActions postNotification(String body) throws Exception {
    return mockMvc.perform(post("/api/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }
}
