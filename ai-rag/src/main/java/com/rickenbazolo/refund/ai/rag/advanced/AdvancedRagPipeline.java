package com.rickenbazolo.refund.ai.rag.advanced;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class AdvancedRagPipeline {

  private final VectorStore vectorStore;
  private final RerankingStrategy reranker;
  private final RagAnswerGenerator answerGenerator;

  public AdvancedRagPipeline(
      VectorStore vectorStore, RerankingStrategy reranker, RagAnswerGenerator answerGenerator) {
    this.vectorStore = vectorStore;
    this.reranker = reranker;
    this.answerGenerator = answerGenerator;
  }

  public AdvancedRagAnswer ask(String question, boolean useReranking) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("Question must not be blank");
    }

    long start = System.nanoTime();
    var search =
        SearchRequest.builder()
            .query(question)
            .topK(10)
            .similarityThreshold(0.0)
            .filterExpression("category == 'refund-policy'")
            .build();
    var retrieved = Optional.ofNullable(vectorStore.similaritySearch(search)).orElseGet(List::of);
    long afterRetrieval = System.nanoTime();

    var candidates =
        IntStream.range(0, retrieved.size())
            .mapToObj(
                i -> {
                  var document = retrieved.get(i);
                  return new RagCandidate(
                      document, document.getScore() == null ? 0.0 : document.getScore(), i + 1);
                })
            .toList();

    List<RankedDocument> selected;
    if (useReranking) {
      selected = reranker.rerank(question, candidates, 3);
    } else {
      selected =
          IntStream.range(0, Math.min(3, candidates.size()))
              .mapToObj(
                  i ->
                      new RankedDocument(
                          candidates.get(i), candidates.get(i).retrievalScore(), i + 1))
              .toList();
    }
    long afterReranking = System.nanoTime();

    String answer =
        selected.isEmpty()
            ? "Aucune politique pertinente n'a été retrouvée."
            : answerGenerator.generate(question, buildContext(selected));
    long end = System.nanoTime();

    var traces =
        selected.stream()
            .map(
                doc ->
                    new AdvancedRagAnswer.DocumentTrace(
                        doc.candidate().policyId(),
                        doc.candidate().originalRank(),
                        doc.finalRank(),
                        doc.candidate().retrievalScore(),
                        doc.rerankingScore(),
                        doc.candidate().document().getText()))
            .toList();

    return new AdvancedRagAnswer(
        question,
        answer,
        traces,
        new AdvancedRagAnswer.Timing(
            millis(afterRetrieval - start),
            millis(afterReranking - afterRetrieval),
            millis(end - afterReranking),
            millis(end - start)));
  }

  private String buildContext(List<RankedDocument> documents) {
    StringBuilder context = new StringBuilder();
    for (var document : documents) {
      context
          .append("\n[")
          .append(document.candidate().policyId())
          .append("]\n")
          .append(document.candidate().document().getText())
          .append("\n");
    }
    return context.toString();
  }

  private long millis(long nanos) {
    return TimeUnit.NANOSECONDS.toMillis(nanos);
  }
}
