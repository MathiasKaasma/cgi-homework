package com.example.homework.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.homework.notification.sender.EmailSender;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LegacyNotificationEngineIntegrationTest {

  @Autowired
  private LegacyNotificationEngine engine;

  @MockitoSpyBean
  private EmailSender emailSender;

  @ParameterizedTest
  @CsvSource({
      "EMAIL, buyer@example.com",
      "SMS,   +37255512345",
      "PUSH,  device-token-abc123"
  })
  void supportedTypeSuccessIsStoredAsSent(NotificationType type, String target) throws Exception {
    LegacyNotificationResponse response = engine.processNotification(type, target, "hello");

    assertThat(response.status()).isEqualTo("S");
    assertThat(response.type()).isEqualTo(type.name());
    assertThat(engine.getHistory()).hasSize(1);
  }

  @Test
  void fourthAttemptToSameRecipientWithinAMinuteIsRateLimited() throws Exception {
    for (int i = 0; i < 3; i++) {
      engine.processNotification(NotificationType.EMAIL, "busy-user@example.com", "hello");
    }

    assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, "busy-user@example.com", "hello"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");

    LegacyNotificationResponse otherRecipient = engine.processNotification(NotificationType.EMAIL, "other-user@example.com", "hello");
    assertThat(otherRecipient.status()).isEqualTo("S");
  }

  @Test
  void blockedTargetFailsBeforeDispatchIsInserted() {
    assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, "blocked-user@example.com", "hello"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");

    assertThat(engine.getHistory()).isEmpty();
  }

  @Test
  void threeFailuresOpenCircuitBreakerForNextAttempt() {
    doThrow(new RuntimeException("SMTP Timeout")).when(emailSender).send(any(), any());

    for (int i = 0; i < 3; i++) {
        assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, "example-user@example.com", "boom"))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("SMTP Timeout");
    }

    assertThatThrownBy(() -> engine.processNotification(NotificationType.SMS, "+37255500000", "hello"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Circuit breaker OPEN via database state.");

    LegacyNotificationResponse latest = engine.getLatestState();
    assertThat(latest.status()).isEqualTo("R");
    assertThat(latest.failureCount()).isEqualTo(3);
    assertThat(latest.dispatchCount()).isEqualTo(4);
  }

  @ParameterizedTest
  @CsvSource({
      "EMAIL, broken-email@",
      "SMS,   +5555broken-sms",
      "PUSH,  "
  })
  void invalidTargetThrowsInvalidRecipientException(NotificationType type, String target) {
    assertThatThrownBy(() -> engine.processNotification(type, target, "hello"))
        .isInstanceOf(InvalidRecipientException.class);

    assertThat(engine.getHistory()).isEmpty();
  }
}
