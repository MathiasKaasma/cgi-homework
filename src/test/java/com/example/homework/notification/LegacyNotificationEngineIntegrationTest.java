package com.example.homework.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.time.Clock;
import java.time.Instant;

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

  @MockitoSpyBean
  private Clock clock;

  @ParameterizedTest
  @CsvSource({
      "EMAIL, buyer@example.com",
      "SMS,   +37255512345",
      "PUSH,  device-token-abc123"
  })
  void supportedTypeSuccessIsStoredAsSent(NotificationType type, String target) throws Exception {
    LegacyNotificationResponse response = engine.processNotification(type, target, "hello");

    assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
    assertThat(response.type()).isEqualTo(type);
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
    assertThat(otherRecipient.status()).isEqualTo(NotificationStatus.SENT);
  }

  @Test
  void blockedTargetFailsBeforeDispatchIsInserted() {
    assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, "blocked-user@example.com", "hello"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");

    assertThat(engine.getHistory()).isEmpty();
  }

  @Test
  void threeFailuresOpenCircuitOnlyForThatChannel() throws Exception {
    failEmailThreeTimes();

    assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, "next@example.com", "hello"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Circuit breaker OPEN via database state.");
    assertThat(engine.getLatestState().status()).isEqualTo(NotificationStatus.REJECTED);

    LegacyNotificationResponse sms = engine.processNotification(NotificationType.SMS, "+37255500000", "hello");
    assertThat(sms.status()).isEqualTo(NotificationStatus.SENT);
  }

  @Test
  void circuitClosesAfterCooldown() throws Exception {
    failEmailThreeTimes();
    doCallRealMethod().when(emailSender).send(any(), any());
    doReturn(Instant.now().plusSeconds(31)).when(clock).instant();

    LegacyNotificationResponse response = engine.processNotification(NotificationType.EMAIL, "later@example.com", "hello");

    assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
  }

  private void failEmailThreeTimes() {
    doThrow(new RuntimeException("SMTP Timeout")).when(emailSender).send(any(), any());

    for (int i = 0; i < 3; i++) {
      String recipient = "user" + i + "@example.com";
      assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, recipient, "boom"))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("SMTP Timeout");
    }
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
