package com.rickenbazolo.refund.api.reliability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rickenbazolo.refund.ai.chat.RefundAiService;
import com.rickenbazolo.refund.api.observability.AiExecutionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.TransientAiException;

class SafeRefundAiServiceTest {

  @Test
  void shouldRejectBlankMessageWithoutFallback() {
    RefundAiService ai = mock(RefundAiService.class);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var registry = new SimpleMeterRegistry();
      var service =
          new SafeRefundAiService(
              ai,
              new AiExecutionGuard(executor, Duration.ofSeconds(1)),
              new AiExecutionMetrics(registry));

      assertThrows(IllegalArgumentException.class, () -> service.ask(" "));
      assertEquals(
          0.0, registry.counter("refund.ai.fallback.total", "reason", "AI_UNAVAILABLE").count());
    }
  }

  @Test
  void shouldReturnModelResponseAndRecordScenarioDuration() {
    RefundAiService ai = mock(RefundAiService.class);
    when(ai.chat("Explain refunds")).thenReturn("Contact support for a review.");
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var registry = new SimpleMeterRegistry();
      var service =
          new SafeRefundAiService(
              ai,
              new AiExecutionGuard(executor, Duration.ofSeconds(1)),
              new AiExecutionMetrics(registry));

      var result = service.ask("Explain refunds");

      assertEquals("SUCCESS", result.status());
      assertTrue(result.generatedByModel());
      assertEquals(
          1.0, registry.get("refund.ai.scenario.duration").tag("scenario", "CHAT").timer().count());
      assertEquals(
          0.0, registry.counter("refund.ai.fallback.total", "reason", "AI_UNAVAILABLE").count());
    }
  }

  @Test
  void shouldFallbackForProviderFailureAndKeepItDistinctFromValidation() {
    RefundAiService ai = mock(RefundAiService.class);
    when(ai.chat("Explain refunds")).thenThrow(new TransientAiException("provider offline"));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var registry = new SimpleMeterRegistry();
      var service =
          new SafeRefundAiService(
              ai,
              new AiExecutionGuard(executor, Duration.ofSeconds(1)),
              new AiExecutionMetrics(registry));

      var result = service.ask("Explain refunds");

      assertEquals("AI_UNAVAILABLE", result.status());
      assertFalse(result.generatedByModel());
      assertEquals(
          1.0, registry.counter("refund.ai.fallback.total", "reason", "AI_UNAVAILABLE").count());
      assertEquals(
          1.0, registry.get("refund.ai.scenario.duration").tag("outcome", "error").timer().count());
    }
  }

  @Test
  void shouldNotTurnAuthorizationFailureIntoFallback() {
    RefundAiService ai = mock(RefundAiService.class);
    when(ai.chat("Explain refunds")).thenThrow(new SecurityException("Access denied"));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var registry = new SimpleMeterRegistry();
      var service =
          new SafeRefundAiService(
              ai,
              new AiExecutionGuard(executor, Duration.ofSeconds(1)),
              new AiExecutionMetrics(registry));

      assertThrows(SecurityException.class, () -> service.ask("Explain refunds"));
      assertEquals(
          0.0, registry.counter("refund.ai.fallback.total", "reason", "AI_UNAVAILABLE").count());
      assertEquals(
          1.0, registry.get("refund.ai.scenario.duration").tag("outcome", "error").timer().count());
    }
  }
}
