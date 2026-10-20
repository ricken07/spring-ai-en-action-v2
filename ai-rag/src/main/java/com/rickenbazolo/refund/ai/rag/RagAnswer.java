package com.rickenbazolo.refund.ai.rag;

import java.util.List;

public record RagAnswer(String question, String answer, List<RetrievedPolicy> retrievedPolicies) {

  public RagAnswer {
    retrievedPolicies = List.copyOf(retrievedPolicies);
  }

  public record RetrievedPolicy(String policyId, String source, String excerpt, Double score) {}
}
