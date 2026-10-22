package com.rickenbazolo.refund.api.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;

@Component
public class AiExecutionMetrics {

  private final MeterRegistry registry;

  public AiExecutionMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public <T> T measure(String scenario, Callable<T> action) throws Exception {
    if (!java.util.Set.of(
            "CHAT",
            "STRUCTURED",
            "MEMORY",
            "RAG",
            "RAG_ADVANCED",
            "TOOLS",
            "MCP",
            "WORKFLOW",
            "AGENT",
            "SAFE_CHAT")
        .contains(scenario)) {
      throw new IllegalArgumentException("Unsupported AI scenario");
    }
    Timer.Sample sample = Timer.start(registry);
    String outcome = "success";
    try {
      return action.call();
    } catch (Exception exception) {
      outcome = "error";
      throw exception;
    } finally {
      sample.stop(
          Timer.builder("refund.ai.scenario.duration")
              .tag("scenario", scenario)
              .tag("outcome", outcome)
              .register(registry));
    }
  }

  public void recordFallback(String reason) {
    if (!"AI_UNAVAILABLE".equals(reason)) {
      throw new IllegalArgumentException("Unsupported fallback reason");
    }
    registry.counter("refund.ai.fallback.total", "reason", reason).increment();
  }
}
