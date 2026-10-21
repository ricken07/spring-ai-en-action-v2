package com.rickenbazolo.refund.api.reliability;

import com.rickenbazolo.refund.ai.chat.RefundAiService;
import com.rickenbazolo.refund.api.observability.AiExecutionMetrics;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
public class SafeRefundAiService {

  private final RefundAiService aiService;
  private final AiExecutionGuard guard;
  private final AiExecutionMetrics metrics;

  public SafeRefundAiService(
      RefundAiService aiService, AiExecutionGuard guard, AiExecutionMetrics metrics) {
    this.aiService = aiService;
    this.guard = guard;
    this.metrics = metrics;
  }

  public SafeAiResponse ask(String message) {
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Message must not be blank");
    }
    try {
      return metrics.measure(
          "CHAT",
          () ->
              SafeAiResponse.success(
                  guard.execute(
                      () -> {
                        try {
                          return aiService.chat(message);
                        } catch (TransientAiException | ResourceAccessException exception) {
                          throw new AiExecutionException("AI provider call failed", exception);
                        }
                      })));
    } catch (AiExecutionException exception) {
      metrics.recordFallback("AI_UNAVAILABLE");
      return SafeAiResponse.unavailable();
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("Unexpected execution failure", exception);
    }
  }
}
