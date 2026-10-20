package com.rickenbazolo.refund.ai.chat;

import com.rickenbazolo.refund.domain.RefundIntent;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RefundAiService {

  private final RefundChatGateway gateway;
  private final RefundIntentValidator validator;

  public RefundAiService(RefundChatGateway gateway, RefundIntentValidator validator) {
    this.gateway = gateway;
    this.validator = validator;
  }

  public String chat(String message) {
    requireMessage(message);
    return gateway.chat(message);
  }

  public IntentAnalysis analyze(String message) {
    requireMessage(message);

    RefundIntent intent = gateway.extractIntent(message);
    List<String> errors = validator.validate(intent);

    return new IntentAnalysis(intent, errors.isEmpty(), errors);
  }

  private void requireMessage(String message) {
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Message must not be blank");
    }
  }

  public record IntentAnalysis(RefundIntent intent, boolean valid, List<String> errors) {}
}
