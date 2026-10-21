package com.rickenbazolo.refund.api.reliability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AiExecutionGuardTest {

  @Test
  void shouldReturnSuccessfulResult() {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var guard = new AiExecutionGuard(executor, Duration.ofSeconds(1));
      assertEquals("OK", guard.execute(() -> "OK"));
    }
  }

  @Test
  void shouldTimeoutAndInterruptReadOnlyAction() throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var entered = new CountDownLatch(1);
      var interrupted = new CountDownLatch(1);
      var guard = new AiExecutionGuard(executor, Duration.ofMillis(100));

      assertThrows(
          AiExecutionException.class,
          () ->
              guard.execute(
                  () -> {
                    entered.countDown();
                    try {
                      new CountDownLatch(1).await();
                      return "unreachable";
                    } catch (InterruptedException exception) {
                      interrupted.countDown();
                      Thread.currentThread().interrupt();
                      throw new IllegalStateException(exception);
                    }
                  }));

      assertEquals(true, entered.await(1, TimeUnit.SECONDS));
      assertEquals(true, interrupted.await(1, TimeUnit.SECONDS));
    }
  }
}
