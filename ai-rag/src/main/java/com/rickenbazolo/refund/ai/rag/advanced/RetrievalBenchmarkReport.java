package com.rickenbazolo.refund.ai.rag.advanced;

public record RetrievalBenchmarkReport(
    int evaluatedQuestions, Metrics withoutReranking, Metrics withReranking) {

  public record Metrics(
      double hitAt1, double mrrAt3, long retrievalMs, long rerankingMs, long totalMs) {}
}
