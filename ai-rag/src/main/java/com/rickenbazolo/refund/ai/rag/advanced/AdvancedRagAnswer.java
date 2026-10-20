package com.rickenbazolo.refund.ai.rag.advanced;

import java.util.List;

public record AdvancedRagAnswer(
    String question, String answer, List<DocumentTrace> documents, Timing timing) {

  public AdvancedRagAnswer {
    documents = List.copyOf(documents);
  }

  public record DocumentTrace(
      String policyId,
      int originalRank,
      int finalRank,
      double retrievalScore,
      double rerankingScore,
      String excerpt) {}

  public record Timing(long retrievalMs, long rerankingMs, long generationMs, long totalMs) {}
}
