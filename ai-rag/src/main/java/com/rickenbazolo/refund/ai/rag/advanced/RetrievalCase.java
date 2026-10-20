package com.rickenbazolo.refund.ai.rag.advanced;

import java.util.Set;

public record RetrievalCase(String id, String question, Set<String> expectedPolicyIds) {

  public RetrievalCase {
    expectedPolicyIds = Set.copyOf(expectedPolicyIds);
  }
}
