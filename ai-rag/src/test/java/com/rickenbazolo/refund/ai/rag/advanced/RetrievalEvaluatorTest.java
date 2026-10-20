package com.rickenbazolo.refund.ai.rag.advanced;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RetrievalEvaluatorTest {

  private final RetrievalEvaluator evaluator = new RetrievalEvaluator();

  @Test
  void shouldComputeHitAtKAndReciprocalRank() {
    var ranked = List.of("P1", "P2", "P3");

    assertEquals(0.0, evaluator.hitAtK(Set.of("P2"), ranked, 1));
    assertEquals(1.0, evaluator.hitAtK(Set.of("P2"), ranked, 2));
    assertEquals(0.5, evaluator.reciprocalRank(Set.of("P2"), ranked, 3));
    assertEquals(0.0, evaluator.reciprocalRank(Set.of("P2"), ranked, 1));
  }

  @Test
  void shouldComputeMeanReciprocalRank() {
    var cases =
        List.of(
            new RetrievalCase("Q1", "one", Set.of("P1")),
            new RetrievalCase("Q2", "two", Set.of("P2")));

    assertEquals(
        0.75, evaluator.meanReciprocalRank(cases, List.of(List.of("P1"), List.of("P1", "P2")), 3));
  }

  @Test
  void shouldRejectInvalidKAndDataset() {
    assertThrows(IllegalArgumentException.class, () -> evaluator.hitAtK(Set.of("P"), List.of(), 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> evaluator.meanReciprocalRank(List.of(), List.of(), 3));
  }
}
