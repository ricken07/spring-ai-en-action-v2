package com.rickenbazolo.refund.ai.rag.advanced;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RetrievalBenchmarkService {

  private static final int CANDIDATE_LIMIT = 10;
  private static final int RESULT_LIMIT = 3;

  private final VectorStore vectorStore;
  private final RerankingStrategy reranker;
  private final RetrievalEvaluator evaluator;
  private final RetrievalEvaluationDataset dataset;

  public RetrievalBenchmarkService(
      VectorStore vectorStore,
      RerankingStrategy reranker,
      RetrievalEvaluator evaluator,
      RetrievalEvaluationDataset dataset) {
    this.vectorStore = vectorStore;
    this.reranker = reranker;
    this.evaluator = evaluator;
    this.dataset = dataset;
  }

  public RetrievalBenchmarkReport evaluate() {
    var cases = dataset.cases();
    var baselineResults = new ArrayList<List<String>>();
    var rerankedResults = new ArrayList<List<String>>();
    long retrievalNanos = 0;
    long rerankingNanos = 0;

    for (var testCase : cases) {
      long retrievalStart = System.nanoTime();
      var request =
          SearchRequest.builder()
              .query(testCase.question())
              .topK(CANDIDATE_LIMIT)
              .similarityThreshold(0.0)
              .filterExpression("category == 'refund-policy'")
              .build();
      var docs = vectorStore.similaritySearch(request);
      retrievalNanos += System.nanoTime() - retrievalStart;

      var candidates =
          IntStream.range(0, docs.size())
              .mapToObj(
                  i -> {
                    var doc = docs.get(i);
                    return new RagCandidate(
                        doc, doc.getScore() == null ? 0.0 : doc.getScore(), i + 1);
                  })
              .toList();
      baselineResults.add(
          candidates.stream().limit(RESULT_LIMIT).map(RagCandidate::policyId).toList());

      long rerankingStart = System.nanoTime();
      var reranked = reranker.rerank(testCase.question(), candidates, RESULT_LIMIT);
      rerankingNanos += System.nanoTime() - rerankingStart;
      rerankedResults.add(reranked.stream().map(doc -> doc.candidate().policyId()).toList());
    }

    double hitAt1Baseline = averageHitAt1(cases, baselineResults);
    double hitAt1Reranked = averageHitAt1(cases, rerankedResults);
    double mrrBaseline = evaluator.meanReciprocalRank(cases, baselineResults, RESULT_LIMIT);
    double mrrReranked = evaluator.meanReciprocalRank(cases, rerankedResults, RESULT_LIMIT);
    long retrievalMs = millis(retrievalNanos);
    long rerankingMs = millis(rerankingNanos);

    return new RetrievalBenchmarkReport(
        cases.size(),
        new RetrievalBenchmarkReport.Metrics(
            hitAt1Baseline, mrrBaseline, retrievalMs, 0, retrievalMs),
        new RetrievalBenchmarkReport.Metrics(
            hitAt1Reranked, mrrReranked, retrievalMs, rerankingMs, retrievalMs + rerankingMs));
  }

  private double averageHitAt1(List<RetrievalCase> cases, List<List<String>> results) {
    double total = 0.0;
    for (int i = 0; i < cases.size(); i++) {
      total += evaluator.hitAtK(cases.get(i).expectedPolicyIds(), results.get(i), 1);
    }
    return total / cases.size();
  }

  private long millis(long nanos) {
    return TimeUnit.NANOSECONDS.toMillis(nanos);
  }
}
