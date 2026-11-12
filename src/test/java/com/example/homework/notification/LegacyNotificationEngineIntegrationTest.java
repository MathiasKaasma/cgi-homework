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
  void supportedTypeSuccessIsStoredAsSent(NotificationType type, String recipient) {
    LegacyNotificationResponse response = engine.processNotification(type, recipient, "hello");

    assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
    assertThat(response.type()).isEqualTo(type);
    assertThat(engine.getHistory()).hasSize(1);
  }

  @Test
  void fourthAttemptToSameRecipientWithinAMinuteIsRateLimited() {
    for (int i = 0; i < 3; i++) {
      engine.processNotification(NotificationType.EMAIL, "busy-user@example.com", "hello");
    }

    assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, "busy-user@example.com", "hello"))
        .isInstanceOf(RateLimitExceededException.class);
    assertThat(engine.getLatestState().status()).isEqualTo(NotificationStatus.REJECTED);

    LegacyNotificationResponse otherRecipient = engine.processNotification(NotificationType.EMAIL, "other-user@example.com", "hello");
    assertThat(otherRecipient.status()).isEqualTo(NotificationStatus.SENT);
  }

  @Test
  void rejectedAttemptsDoNotCountTowardsRateLimit() {
    failEmailThreeTimes();
    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, "busy-user@example.com", "hello"))
          .isInstanceOf(CircuitOpenException.class);
    }

    doCallRealMethod().when(emailSender).send(any(), any());
    doReturn(Instant.now().plusSeconds(31)).when(clock).instant();

    LegacyNotificationResponse response = engine.processNotification(NotificationType.EMAIL, "busy-user@example.com", "hello");
    assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
  }

  @Test
  void blacklistedRecipientIsRejectedAndStored() {
    assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, "blocked-user@example.com", "hello"))
        .isInstanceOf(RecipientBlacklistedException.class)
        .hasMessage("Recipient 'blocked-user@example.com' is blacklisted");

    LegacyNotificationResponse latest = engine.getLatestState();
    assertThat(latest.status()).isEqualTo(NotificationStatus.REJECTED);
    assertThat(latest.detail()).isEqualTo("Recipient 'blocked-user@example.com' is blacklisted");
  }

  @Test
  void threeFailuresOpenCircuitOnlyForThatChannel() {
    failEmailThreeTimes();

    assertThatThrownBy(() -> engine.processNotification(NotificationType.EMAIL, "next@example.com", "hello"))
        .isInstanceOf(CircuitOpenException.class);
    assertThat(engine.getLatestState().status()).isEqualTo(NotificationStatus.REJECTED);

    LegacyNotificationResponse sms = engine.processNotification(NotificationType.SMS, "+37255500000", "hello");
    assertThat(sms.status()).isEqualTo(NotificationStatus.SENT);
  }

  @Test
  void circuitClosesAfterCooldown() {
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
  void invalidRecipientIsRejectedAndNotStored(NotificationType type, String recipient) {
    assertThatThrownBy(() -> engine.processNotification(type, recipient, "hello"))
        .isInstanceOf(InvalidRecipientException.class);

    assertThat(engine.getHistory()).isEmpty();
  }
}
