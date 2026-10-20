package com.rickenbazolo.refund.ai.rag.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class AdvancedRagPipelineTest {

  private final VectorStore vectorStore = mock(VectorStore.class);

  @Test
  void shouldRetrieveOnceAndGenerateFromOnlySelectedDocuments() {
    var generator = new CapturingGenerator();
    var reranker = new SelectLastCandidateReranker();
    when(vectorStore.similaritySearch(any(SearchRequest.class)))
        .thenReturn(
            List.of(
                document("first context", "P1"),
                document("second context", "P2"),
                document("third context", "P3"),
                document("excluded context", "P4")));

    var answer = new AdvancedRagPipeline(vectorStore, reranker, generator).ask("question", true);

    verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    assertEquals("generated answer", answer.answer());
    assertEquals(3, answer.documents().size());
    assertTrue(generator.context.contains("[P3]"));
    assertTrue(generator.context.contains("third context"));
    assertFalse(generator.context.contains("excluded context"));
    assertEquals("P3", answer.documents().getFirst().policyId());
    assertEquals(3, answer.documents().getFirst().originalRank());
    assertEquals(1, answer.documents().getFirst().finalRank());
    assertNonNegative(answer.timing());
  }

  @Test
  void shouldSkipRerankingWhenDisabled() {
    var reranker = mock(RerankingStrategy.class);
    var generator = new CapturingGenerator();
    when(vectorStore.similaritySearch(any(SearchRequest.class)))
        .thenReturn(List.of(document("only context", "P1")));

    new AdvancedRagPipeline(vectorStore, reranker, generator).ask("question", false);

    verifyNoInteractions(reranker);
    assertTrue(generator.context.contains("only context"));
  }

  @Test
  void shouldNotCallGeneratorWhenRetrievalIsEmpty() {
    var generator = mock(RagAnswerGenerator.class);
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

    var answer =
        new AdvancedRagPipeline(vectorStore, mock(RerankingStrategy.class), generator)
            .ask("question", true);

    verifyNoInteractions(generator);
    assertTrue(answer.documents().isEmpty());
    assertEquals("Aucune politique pertinente n'a été retrouvée.", answer.answer());
    assertNonNegative(answer.timing());
  }

  @Test
  void shouldRejectBlankQuestionBeforeRetrieval() {
    var pipeline =
        new AdvancedRagPipeline(
            vectorStore, mock(RerankingStrategy.class), mock(RagAnswerGenerator.class));

    assertThrows(IllegalArgumentException.class, () -> pipeline.ask(" ", false));
    verifyNoInteractions(vectorStore);
  }

  private Document document(String text, String id) {
    return new Document(text, Map.of("policyId", id));
  }

  private void assertNonNegative(AdvancedRagAnswer.Timing timing) {
    assertTrue(timing.retrievalMs() >= 0);
    assertTrue(timing.rerankingMs() >= 0);
    assertTrue(timing.generationMs() >= 0);
    assertTrue(timing.totalMs() >= 0);
  }

  private static class CapturingGenerator implements RagAnswerGenerator {
    private String context;

    @Override
    public String generate(String question, String context) {
      this.context = context;
      return "generated answer";
    }
  }

  private static class SelectLastCandidateReranker implements RerankingStrategy {
    @Override
    public List<RankedDocument> rerank(String query, List<RagCandidate> candidates, int limit) {
      var selected = candidates.stream().limit(limit).toList();
      return java.util.stream.IntStream.range(0, selected.size())
          .mapToObj(
              i -> {
                var candidate = selected.get(selected.size() - 1 - i);
                return new RankedDocument(candidate, 1.0 - i * 0.1, i + 1);
              })
          .toList();
    }
  }
}
