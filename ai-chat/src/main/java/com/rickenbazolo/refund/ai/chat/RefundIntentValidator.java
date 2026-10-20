package com.rickenbazolo.refund.ai.chat;

import com.rickenbazolo.refund.domain.RefundIntent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RefundIntentValidator {

  private static final Set<String> ACTIONS = Set.of("REFUND", "INFORMATION", "OTHER");

  public List<String> validate(RefundIntent intent) {
    List<String> errors = new ArrayList<>();

    if (intent == null) {
      return List.of("Intent must not be null");
    }

    if (intent.requestedAction() == null || !ACTIONS.contains(intent.requestedAction())) {
      errors.add("Unsupported requested action");
    }

    if (intent.reason() == null || intent.reason().isBlank()) {
      errors.add("Missing request reason");
    }

    if (intent.orderId() == null || intent.orderId().isBlank()) {
      errors.add("Missing order identifier");
    }

    return List.copyOf(errors);
  }
}
