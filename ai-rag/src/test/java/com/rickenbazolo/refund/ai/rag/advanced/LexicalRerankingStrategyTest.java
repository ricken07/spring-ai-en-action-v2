package com.rickenbazolo.refund.ai.rag.advanced;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class LexicalRerankingStrategyTest {

  private final RerankingStrategy reranker = new LexicalRerankingStrategy();

  @Test
  void shouldPromoteMatchingDocumentAndPreserveOriginalRank() {
    var candidates =
        List.of(
            candidate("Le délai de retour est de 30 jours.", "WINDOW", 1),
            candidate("La facture est un justificatif d'achat.", "PROOF", 2));

    var ranked = reranker.rerank("Quels justificatifs d'achat ?", candidates, 2);

    assertEquals("PROOF", ranked.getFirst().candidate().policyId());
    assertEquals(2, ranked.getFirst().candidate().originalRank());
    assertEquals(1, ranked.getFirst().finalRank());
  }

  @Test
  void shouldNormalizeAccentsAndApplyLimit() {
    var ranked =
        reranker.rerank(
            "Quels justificatifs acceptés ?",
            List.of(
                candidate("Un document sans rapport.", "OTHER", 1),
                candidate("Les justificatifs acceptés sont la facture.", "PROOF", 2)),
            1);

    assertEquals(1, ranked.size());
    assertEquals("PROOF", ranked.getFirst().candidate().policyId());
  }

  @Test
  void shouldRejectNonPositiveLimit() {
    assertThrows(IllegalArgumentException.class, () -> reranker.rerank("question", List.of(), 0));
  }

  private RagCandidate candidate(String text, String policyId, int rank) {
    return new RagCandidate(new Document(text, Map.of("policyId", policyId)), 0.8, rank);
  }
}
