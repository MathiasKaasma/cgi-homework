package com.example.homework.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LegacyNotificationEngineIntegrationTest {

  @Autowired
  private LegacyNotificationEngine engine;

  @Test
  void emailSuccessIsStoredAsSent() throws Exception {
    LegacyNotificationResponse response = engine.processNotification("EMAIL", "buyer@example.com", "hello");

    assertThat(response.getStatus()).isEqualTo("S");
    assertThat(response.getDispatchCount()).isEqualTo(1);
    assertThat(response.getFailureCount()).isZero();
    assertThat(engine.getHistory()).hasSize(1);
  }

  @Test
  void blockedTargetFailsBeforeDispatchIsInserted() {
    assertThatThrownBy(() -> engine.processNotification("SMS", "blocked-user@example.com", "hello"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");

    assertThat(engine.getHistory()).isEmpty();
  }

  @Test
  void unsupportedTypeIsStoredAsUnknown() {
    assertThatThrownBy(() -> engine.processNotification("FAX", "ops@example.com", "hello"))
        .isInstanceOf(Exception.class)
        .hasMessage("Unsupported type: FAX");

    LegacyNotificationResponse latest = engine.getLatestState();
    assertThat(latest.getStatus()).isEqualTo("U");
    assertThat(latest.getMessage()).isEqualTo("Unsupported type: FAX");
  }

  @Test
  void threeFailuresOpenCircuitBreakerForNextAttempt() {
    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(() -> engine.processNotification("EMAIL", "error-user@example.com", "boom"))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("SMTP Timeout");
    }

    assertThatThrownBy(() -> engine.processNotification("SMS", "ok-user@example.com", "hello"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Circuit breaker OPEN via database state.");

    LegacyNotificationResponse latest = engine.getLatestState();
    assertThat(latest.getStatus()).isEqualTo("R");
    assertThat(latest.getFailureCount()).isEqualTo(3);
    assertThat(latest.getDispatchCount()).isEqualTo(4);
  }
}
