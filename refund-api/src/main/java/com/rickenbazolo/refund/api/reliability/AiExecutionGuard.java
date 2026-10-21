package com.rickenbazolo.refund.api.reliability;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AiExecutionGuard {

  private final ExecutorService executor;
  private final Duration timeout;

  public AiExecutionGuard(ExecutorService executor, Duration timeout) {
    if (executor == null || timeout == null || timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("Executor and positive timeout are required");
    }
    this.executor = executor;
    this.timeout = timeout;
  }

  public <T> T execute(Callable<T> action) {
    Future<T> future = executor.submit(action);
    try {
      return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException exception) {
      future.cancel(true);
      throw new AiExecutionException("AI execution timed out", exception);
    } catch (InterruptedException exception) {
      future.cancel(true);
      Thread.currentThread().interrupt();
      throw new AiExecutionException("AI execution interrupted", exception);
    } catch (ExecutionException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new AiExecutionException("AI execution failed", cause);
    }
  }
}
