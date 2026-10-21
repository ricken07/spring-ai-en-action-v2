package com.rickenbazolo.refund.ai.workflows;

import java.util.List;

public record RefundAgentResult(String answer, String executionMode, List<String> invokedTools) {
  public RefundAgentResult {
    invokedTools = List.copyOf(invokedTools);
  }
}
