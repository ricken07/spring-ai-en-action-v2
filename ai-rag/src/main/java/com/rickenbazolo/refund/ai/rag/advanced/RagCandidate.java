package com.rickenbazolo.refund.ai.rag.advanced;

import org.springframework.ai.document.Document;

public record RagCandidate(Document document, double retrievalScore, int originalRank) {

  public String policyId() {
    return String.valueOf(document.getMetadata().getOrDefault("policyId", ""));
  }
}
